package offline.subcommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.ByteString;
import core.gen.rpc.CallbackRequest;
import core.gen.rpc.CallbackType;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponseAlternative;
import core.gen.rpc.EditorState;
import core.gen.rpc.EditorStateRequest;
import core.gen.rpc.EvaluateRequest;
import core.gen.rpc.EvaluateResponse;
import core.gen.rpc.EvaluateTextRequest;
import core.gen.rpc.InitializeRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import offline.Subcommand;
import toolbelt.client.CoreClient;
import toolbelt.languages.LanguageDeterminer;

@Singleton
public class TutorialGenerator implements Subcommand {

  class Tutorial {

    List<Step> steps = Arrays.asList();
    public String filename = "";
  }

  class Step {

    public String title;
    public String body;
    public String source;
    public Integer cursor;
    public String transcript;
    public String resetSource;
    public Integer resetCursor;
    public List<String> matches;
    public Boolean generate;
    public Boolean skipEditorFocus;
    public Boolean nextWhenEditorFilename;
    public Boolean nextWhenEditorFocused;
    public Boolean textOnly;
    public Boolean hideAnswer;
    public Boolean last;
    public String description;
  }

  private String clipboard = "";

  @Inject
  LanguageDeterminer languageDeterminer;

  @Inject
  public TutorialGenerator() {}

  private CoreClient createClient() {
    CoreClient client = new CoreClient("localhost:17200");
    client.connect();
    return client;
  }

  private EditorState runStep(CoreClient client, EditorState state, List<Step> steps, int index) {
    Step step = steps.get(index);
    client.send(
      EvaluateRequest
        .newBuilder()
        .setInitializeRequest(InitializeRequest.newBuilder().setEditorState(state).build())
        .build()
    );

    client.send(
      EvaluateRequest
        .newBuilder()
        .setTextRequest(
          EvaluateTextRequest
            .newBuilder()
            .setText(step.transcript)
            .setIncludeAlternatives(true)
            .build()
        )
        .build()
    );

    EvaluateResponse response = client.await();
    CommandsResponseAlternative alternative = response.getCommandsResponse().getExecute();
    if (step.description != null) {
      alternative =
        response
          .getCommandsResponse()
          .getAlternativesList()
          .stream()
          .filter(e -> e.getDescription().equals(step.description))
          .findFirst()
          .get();
    }

    List<Command> commands = alternative.getCommandsList();
    Command command = commands.get(commands.size() - 1);
    String updatedSource = command.getSource();
    int updatedCursor = command.getCursor();

    if (command.getType() == CommandType.COMMAND_TYPE_CLIPBOARD) {
      for (Command c : commands) {
        if (c.getType() == CommandType.COMMAND_TYPE_DIFF) {
          updatedSource = c.getSource();
          updatedCursor = c.getCursor();
        }
      }

      client.send(
        EvaluateRequest
          .newBuilder()
          .setEditorStateRequest(
            EditorStateRequest
              .newBuilder()
              .setEditorState(
                EditorState
                  .newBuilder(state)
                  .setSource(ByteString.copyFromUtf8(updatedSource))
                  .setCursor(updatedCursor)
                  .setClipboard(clipboard)
                  .build()
              )
              .build()
          )
          .build()
      );

      client.send(
        EvaluateRequest
          .newBuilder()
          .setCallbackRequest(
            CallbackRequest.newBuilder().setType(CallbackType.CALLBACK_TYPE_PASTE).build()
          )
          .build()
      );

      response = client.await();
      alternative = response.getCommandsResponse().getExecute();
      commands = alternative.getCommandsList();
      command = commands.get(commands.size() - 1);
      updatedSource = command.getSource();
      updatedCursor = command.getCursor();
    }

    if (command.getType() == CommandType.COMMAND_TYPE_COPY) {
      clipboard = command.getText();
      updatedSource = state.getSource().toStringUtf8();
      updatedCursor = state.getCursor();
    }

    steps.get(index + 1).source = updatedSource;
    steps.get(index + 1).cursor = updatedCursor;
    String description = alternative.getDescription();
    if (!step.transcript.equals(description) && !description.startsWith("Syntax error")) {
      List<String> matches = steps.get(index).matches == null
        ? new ArrayList<>()
        : new ArrayList<>(steps.get(index).matches);
      matches.add(description);
      steps.get(index).matches = matches;
    }

    client.send(
      EvaluateRequest
        .newBuilder()
        .setCallbackRequest(
          CallbackRequest
            .newBuilder()
            .setType(CallbackType.CALLBACK_TYPE_ADD_TO_HISTORY)
            .setText(step.transcript)
            .build()
        )
        .build()
    );

    if (System.getenv("PRINT_STEPS") != null) {
      System.out.println("Step " + (index + 1) + ": " + step.transcript + "\n");
      System.out.println(
        updatedSource.substring(0, updatedCursor) + "<>" + updatedSource.substring(updatedCursor)
      );
      System.out.println("\n===\n");
    }

    return EditorState
      .newBuilder(state)
      .setSource(ByteString.copyFromUtf8(updatedSource))
      .setCursor(updatedCursor)
      .build();
  }

  public String create(String path) {
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    CoreClient client = createClient();
    Tutorial tutorial = null;
    try {
      tutorial = gson.fromJson(Files.readString(Path.of(path)), Tutorial.class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    EditorState state = EditorState
      .newBuilder()
      .setSource(ByteString.copyFromUtf8(""))
      .setCursor(0)
      .setFilename(tutorial.filename)
      .setLanguage(languageDeterminer.fromFilename(tutorial.filename))
      .setApplication("atom")
      .setClientIdentifier("{\"version\": \"1.10.0\", \"os\": \"darwin\"}")
      .setPluginInstalled(true)
      .setCanGetState(true)
      .setCanSetState(true)
      .build();

    for (int i = 0; i < tutorial.steps.size() - 1; i++) {
      Step step = tutorial.steps.get(i);
      if (step.resetSource != null) {
        int cursor = step.resetCursor != null ? step.resetCursor : 0;
        step.source = step.resetSource;
        step.cursor = cursor;
        state =
          EditorState
            .newBuilder(state)
            .setSource(ByteString.copyFromUtf8(step.resetSource))
            .setCursor(cursor)
            .build();
      }

      if (step.textOnly != null && step.transcript == null) {
        step.transcript = "next";
      }

      if (
        step.nextWhenEditorFocused != null ||
        step.nextWhenEditorFilename != null ||
        step.skipEditorFocus != null ||
        step.textOnly != null
      ) {
        continue;
      }

      state = runStep(client, state, tutorial.steps, i);
    }

    tutorial.steps.get(tutorial.steps.size() - 1).last = true;
    client.disconnect();
    return gson.toJson(tutorial);
  }

  @Override
  public void configureSubparsers(Subparsers subparsers) {
    Subparser parser = subparsers.addParser("generate-tutorial").help("Generate a tutorial.");
    parser.addArgument("input").type(String.class).required(true).help("Path to input JSON file");
    parser.addArgument("output").type(String.class).required(true).help("Path to output JSON file");
  }

  @Override
  public void run(Namespace namespace) {
    try {
      Files.write(
        Paths.get(namespace.getString("output")),
        create(namespace.getString("input")).getBytes()
      );
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
