package replayer;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import core.evaluator.ParsedTranscript;
import core.evaluator.Reranker;
import core.evaluator.TranscriptParser;
import core.gen.rpc.CustomCommand;
import core.gen.rpc.EditorState;
import core.metadata.EditorStateWithMetadata;
import core.parser.ParseTree;
import core.util.PhraseHintExtractor;
import core.visitor.BaseVisitor;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import speechengine.gen.rpc.Alternative;
import speechengine.gen.rpc.AlternativesResponse;
import speechengine.gen.rpc.AudioRequest;
import speechengine.gen.rpc.AudioToAlternativesRequest;
import speechengine.gen.rpc.InitRequest;
import speechengine.gen.rpc.TranscriptsRequest;

class TextTagVisitor extends BaseVisitor<Void, Boolean> {

  @Inject
  public TextTagVisitor() {
    register("formattedText", this::visitFormattedText);
  }

  @Override
  protected Boolean aggregateResult(Boolean aggregate, Boolean nextResult) {
    return (aggregate || nextResult);
  }

  @Override
  protected Boolean defaultResult() {
    return false;
  }

  public Boolean visitFormattedText(ParseTree node, Void context) {
    return true;
  }
}

public class Replayer {

  private PhraseHintExtractor phraseHintExtractor;
  private Reranker reranker;
  private TranscriptParser transcriptParser;
  private CompletableFuture<AlternativesResponse> future;
  private String audioDirectory;
  private String stateDirectory;
  private SpeechEngineReplayerClient speechEngineReplayerClient;

  private Gson gson = new Gson();
  private final int chunkSize = 640;

  @AssistedInject
  public Replayer(
    PhraseHintExtractor phraseHintExtractor,
    Reranker reranker,
    TranscriptParser transcriptParser,
    SpeechEngineReplayerClient speechEngineReplayerClient,
    @Assisted("audioDirectory") String audioDirectory,
    @Assisted("stateDirectory") String stateDirectory
  ) {
    this.phraseHintExtractor = phraseHintExtractor;
    this.reranker = reranker;
    this.transcriptParser = transcriptParser;
    this.speechEngineReplayerClient = speechEngineReplayerClient;
    this.speechEngineReplayerClient.setHost("localhost:17202");
    this.audioDirectory = audioDirectory;
    this.stateDirectory = stateDirectory;
  }

  @AssistedFactory
  public interface Factory {
    Replayer create(
      @Assisted("audioDirectory") String audioDirectory,
      @Assisted("stateDirectory") String stateDirectory
    );
  }

  private void createNewConnection() {
    speechEngineReplayerClient.disconnect();
    speechEngineReplayerClient.connect();
  }

  private AlternativesResponse getAlternativesResponse(
    List<String> chunkIds,
    EditorStateWithMetadata state
  ) {
    AlternativesResponse result = null;
    try {
      speechEngineReplayerClient.send(
        AudioToAlternativesRequest
          .newBuilder()
          .setInitRequest(
            InitRequest
              .newBuilder()
              .addAllHints(phraseHintExtractor.extract(state.getSource(), state.getCustomHints()))
              .build()
          )
          .build(),
        true
      );

      for (String chunkId : chunkIds) {
        AudioInputStream inputStream = AudioSystem.getAudioInputStream(
          new File(audioDirectory + "/" + chunkId + ".wav")
        );

        int read = 0;
        while (read != -1) {
          byte[] data = new byte[chunkSize];
          read = inputStream.read(data, 0, chunkSize);
          if (read > -1) {
            speechEngineReplayerClient.send(
              AudioToAlternativesRequest
                .newBuilder()
                .setAudioRequest(
                  AudioRequest
                    .newBuilder()
                    .setAudio(ByteString.copyFrom(Arrays.copyOfRange(data, 0, read)))
                    .build()
                )
                .build(),
              false
            );
          }
        }
      }

      speechEngineReplayerClient.send(
        AudioToAlternativesRequest
          .newBuilder()
          .setTranscriptsRequest(TranscriptsRequest.newBuilder().build())
          .build(),
        false
      );
      result = speechEngineReplayerClient.await();
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Prediction predict(List<String> chunkIds, String stateHash) {
    LoggedEditorState state = new LoggedEditorState();
    try {
      state =
        gson.fromJson(
          Files.readString(Path.of(stateDirectory + "/" + stateHash + ".json")),
          LoggedEditorState.class
        );
    } catch (Exception e) {}

    List<CustomCommand> customCommands = state.custom_commands
      .stream()
      .map(
        c ->
          CustomCommand
            .newBuilder()
            .setTemplated(c.templated)
            .addAllApplications(c.applications)
            .addAllLanguages(c.languages)
            .setGenerated(c.generated)
            .setSnippetType(c.transform)
            .build()
      )
      .collect(Collectors.toList());

    EditorStateWithMetadata editorState = new EditorStateWithMetadata(
      EditorState
        .newBuilder()
        .setCursor(state.cursor)
        .setSource(ByteString.copyFromUtf8(state.source))
        .addAllCustomCommands(customCommands)
        .build()
    );

    AlternativesResponse response = getAlternativesResponse(chunkIds, editorState);
    List<Alternative> alternatives = response.getAlternativesList();
    List<Alternative> rerankedAlternatives = reranker.rerankTranscripts(alternatives, editorState);

    List<String> validTranscripts = rerankedAlternatives
      .stream()
      .map(alt -> transcriptParser.parseWithAntlr(alt, editorState).get(0))
      .filter(p -> p.isValid)
      .map(p -> p.transcript())
      .collect(Collectors.toList());

    List<Double> acousticCosts = new ArrayList<>();
    List<Double> languageModelCosts = new ArrayList<>();
    for (String transcript : validTranscripts) {
      Double acousticCost = 0.0;
      Double languageModelCost = 0.0;
      for (Alternative alternative : alternatives) {
        if (alternative.getTranscript().equals(transcript)) {
          acousticCost = alternative.getAcousticCost();
          languageModelCost = alternative.getLanguageModelCost();
          break;
        }
      }
      acousticCosts.add(acousticCost);
      languageModelCosts.add(languageModelCost);
    }

    return new Prediction(
      response,
      new ArrayList<String>(validTranscripts),
      acousticCosts,
      languageModelCosts
    );
  }

  public void disconnect() {
    speechEngineReplayerClient.disconnect();
  }

  public void updateTextTags(Datapoint datapoint) {
    datapoint.tags.remove("text");
    datapoint.tags.remove("nontext");

    Alternative labelAlternative = Alternative.newBuilder().setTranscript(datapoint.label).build();
    ParsedTranscript parsedLabel = transcriptParser
      .parseWithAntlr(labelAlternative, new EditorStateWithMetadata())
      .get(0);

    datapoint.tags.add(
      new TextTagVisitor().visit(parsedLabel.getCommandChain(), null) ? "text" : "nontext"
    );
  }

  public void setPredictions(List<Datapoint> datapoints) {
    // sort by user ID so that commands by the same user are consecutive
    datapoints.sort((a, b) -> a.user_id.compareTo(b.user_id));
    Gson gson = new Gson();
    String previousUserId = "";

    for (int i = 0; i < datapoints.size(); i++) {
      Datapoint datapoint = datapoints.get(i);
      // to simulate speaker adaptation, use a new connection each time the user ID changes
      if (!datapoint.user_id.equals(previousUserId)) {
        System.out.println("Creating new connection due to new user id.");
        createNewConnection();
      }

      System.out.println(
        "Processing " + (i + 1) + "/" + datapoints.size() + " " + datapoint.chunk_ids
      );
      datapoint.prediction = predict(datapoint.chunk_ids, datapoint.state);
      previousUserId = datapoint.user_id;

      System.out.println("Label: " + datapoint.label);
      System.out.println("Transcripts: " + String.join(", ", datapoint.prediction.transcripts));
      System.out.println("===");
    }
  }

  public static void main(String[] args) throws IOException {
    ArgumentParser parser = ArgumentParsers
      .newFor("Replayer")
      .build()
      .defaultHelp(true)
      .description("Evaluates a speech engine server by simulating audio requests.");

    parser.addArgument("--test-set").type(String.class);
    parser.addArgument("--output").type(String.class);
    parser.addArgument("--audio").type(String.class);
    parser.addArgument("--state").type(String.class);
    Namespace namespace = null;
    try {
      namespace = parser.parseArgs(args);
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.exit(1);
    }

    try {
      new ProcessBuilder(Arrays.asList("pkill", "-f", "speech-engine")).start().waitFor();
      new ProcessBuilder(Arrays.asList("pkill", "-f", "code-engine")).start().waitFor();
      new ProcessBuilder(Arrays.asList("pkill", "-f", "run.py")).start().waitFor();
      Thread.sleep(1000);

      ProcessBuilder builder = new ProcessBuilder(
        Arrays.asList("/root/serenade/scripts/serenade/packaging/run.py", "--skip-build")
      );
      builder.inheritIO();
      builder.start();
      Thread.sleep(30000);
    } catch (InterruptedException e) {}

    ReplayerComponent component = DaggerReplayerComponent.builder().build();
    Replayer.Factory replayerFactory = component.replayerFactory();
    Replayer replayer = replayerFactory.create(
      namespace.getString("audio"),
      namespace.getString("state")
    );

    String output = namespace.getString("output") + "/predictions.jsonl";
    new File(output).delete();
    try {
      Files.createFile(Paths.get(output));
    } catch (FileAlreadyExistsException e) {}

    Gson gson = new Gson();
    List<Datapoint> datapoints = Files
      .readAllLines(Paths.get(namespace.getString("test_set")))
      .stream()
      .map(e -> gson.fromJson(e, Datapoint.class))
      .collect(Collectors.toList());

    replayer.setPredictions(datapoints);

    for (Datapoint datapoint : datapoints) {
      replayer.updateTextTags(datapoint);

      try {
        Files.write(
          Paths.get(output),
          (gson.toJson(datapoint) + "\n").getBytes(),
          StandardOpenOption.APPEND
        );
      } catch (IOException e) {}
    }

    new MetricsPrinter().printAnalysis(datapoints, namespace.getString("output") + "/results.txt");
    replayer.disconnect(); // script doesn't end without this.
  }
}
