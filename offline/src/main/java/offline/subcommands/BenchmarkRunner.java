package offline.subcommands;

import com.google.protobuf.ByteString;
import core.gen.rpc.EditorState;
import core.gen.rpc.EndpointRequest;
import core.gen.rpc.EvaluateAudioRequest;
import core.gen.rpc.EvaluateRequest;
import core.gen.rpc.EvaluateResponse;
import core.gen.rpc.EvaluateTextRequest;
import core.gen.rpc.InitializeRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import offline.Subcommand;
import toolbelt.client.CoreClient;
import toolbelt.env.Env;

@Singleton
public class BenchmarkRunner implements Subcommand {

  @Inject
  Env env;

  @Inject
  public BenchmarkRunner() {}

  private CoreClient createClient() {
    CoreClient client = new CoreClient("localhost:17200");
    client.connect();
    return client;
  }

  private EditorState createEditorState(String file, int cursor) {
    try {
      String content = Files.readString(Path.of(file));
      EditorState state = EditorState
        .newBuilder()
        .setSource(ByteString.copyFromUtf8(content))
        .setCursor(cursor)
        .setFilename(file)
        .setApplication("atom")
        .setClientIdentifier("{\"version\": \"1.10.0\", \"os\": \"darwin\"}")
        .setPluginInstalled(true)
        .setCanGetState(true)
        .setCanSetState(true)
        .build();
      return state;
    } catch (IOException e) {
      return null;
    }
  }

  public void runAudio(String audio, String file, int cursor, int count) {
    CoreClient client = createClient();
    EditorState state = createEditorState(file, cursor);
    long total = 0;
    try {
      for (int i = 0; i < count; i++) {
        System.out.println("Run: " + (i + 1));
        long start = System.currentTimeMillis();
        String chunkId = UUID.randomUUID().toString();
        client.send(
          EvaluateRequest
            .newBuilder()
            .setInitializeRequest(InitializeRequest.newBuilder().setEditorState(state).build())
            .build()
        );
        client.send(
          EvaluateRequest
            .newBuilder()
            .setAudioRequest(
              EvaluateAudioRequest
                .newBuilder()
                .setChunkId(chunkId)
                .setAudio(ByteString.copyFrom(Files.readAllBytes(Path.of(audio))))
                .build()
            )
            .build()
        );
        client.send(
          EvaluateRequest
            .newBuilder()
            .setEndpointRequest(
              EndpointRequest
                .newBuilder()
                .setChunkId(chunkId)
                .setFinalize(true)
                .setEndpointId(UUID.randomUUID().toString())
                .build()
            )
            .build()
        );
        EvaluateResponse response = client.await();
        if (i == 0) {
          System.out.println(response);
        }
        long time = System.currentTimeMillis() - start;
        total += time;
        System.out.println("Time: " + time);
      }
      client.disconnect();
      System.out.println("Average Time: " + (total / count));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void runTranscript(String transcript, String file, int cursor, int count) {
    CoreClient client = createClient();
    EditorState state = createEditorState(file, cursor);
    long total = 0;
    try {
      for (int i = 0; i < count; i++) {
        System.out.println("Run: " + (i + 1));
        long start = System.currentTimeMillis();
        client.send(
          EvaluateRequest
            .newBuilder()
            .setInitializeRequest(InitializeRequest.newBuilder().setEditorState(state).build())
            .build()
        );
        client.send(
          EvaluateRequest
            .newBuilder()
            .setTextRequest(EvaluateTextRequest.newBuilder().setText(transcript).build())
            .build()
        );
        EvaluateResponse response = client.await();
        long time = System.currentTimeMillis() - start;
        total += time;
        if (i == 0) {
          System.out.println(response);
        }
        System.out.println("Time: " + time);
      }
      client.disconnect();
      System.out.println("Average Time: " + (total / count));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void configureSubparsers(Subparsers subparsers) {
    Subparser parser = subparsers
      .addParser("benchmark")
      .help("Execute a text command from an editor state.");
    parser.addArgument("--transcript").type(String.class).help("Transcript to send");
    parser.addArgument("--audio").type(String.class).help("Audio to send");
    parser.addArgument("--file").type(String.class).help("File to use as source");
    parser.addArgument("--cursor").type(Integer.class).help("Cursor position to use");
    parser.addArgument("--count").type(Integer.class).help("Number of times to run");
  }

  @Override
  public void run(Namespace namespace) {
    String audio = namespace.getString("audio");
    String transcript = namespace.getString("transcript");
    if (transcript == null) {
      transcript = "go to parameter";
    }

    String file = namespace.getString("file");
    if (file == null) {
      file = env.sourceRoot() + "/offline/src/test/resources/function1.py";
    }

    Integer cursor = namespace.getInt("cursor");
    if (cursor == null) {
      cursor = 0;
    }

    Integer count = namespace.getInt("count");
    if (count == null) {
      count = 10;
    }

    if (audio == null) {
      runTranscript(transcript, file, cursor, count);
    } else {
      runAudio(audio, file, cursor, count);
    }
  }
}
