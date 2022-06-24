package core.evaluator;

import core.commands.Commands;
import core.commands.CommandsFactory;
import core.exception.NoFileFound;
import core.exception.SafeToDisplayException;
import core.gen.rpc.CallbackRequest;
import core.gen.rpc.CallbackType;
import core.gen.rpc.Change;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponse;
import core.gen.rpc.CommandsResponseAlternative;
import core.metadata.DiffWithMetadata;
import core.metadata.EditorStateWithMetadata;
import core.util.Diff;
import core.util.InsertDirection;
import core.util.Range;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speechengine.gen.rpc.Alternative;
import toolbelt.logging.Logs;
import toolbelt.state.History;

@Singleton
public class CallbackEvaluator {

  private Logger logger = LoggerFactory.getLogger(TranscriptEvaluator.class);

  @Inject
  History history;

  @Inject
  NewlineNormalizer newlineNormalizer;

  @Inject
  CommandsFactory commandsFactory;

  @Inject
  TranscriptEvaluator transcriptEvaluator;

  @Inject
  TranscriptParser transcriptParser;

  @Inject
  Whitespace whitespace;

  @Inject
  public CallbackEvaluator() {}

  private CommandsResponseAlternative createAlternative(
    String transcript,
    String description,
    List<Command> commands
  ) {
    return CommandsResponseAlternative
      .newBuilder()
      .setDescription(description)
      .setTranscript(transcript)
      .setAlternativeId(UUID.randomUUID().toString())
      .addAllCommands(commands)
      .build();
  }

  private Optional<CommandsResponse> handleException(Throwable e) {
    String message = "An error occurred";
    if (e instanceof CompletionException) {
      e = e.getCause();
    }

    // This might be prone to errors that leak info to the user, so be careful.
    boolean displayTranscript = true;
    if (e instanceof SafeToDisplayException) {
      message = e.getMessage();
    } else {
      Logs.logError(
        logger,
        e.getMessage() != null ? e.getMessage() : "CallbackEvaluator error",
        e
      );
    }

    return Optional.of(
      CommandsResponse
        .newBuilder()
        .setTextResponse(true)
        .addAllAlternatives(
          Arrays.asList(
            createAlternative(
              message,
              message,
              Arrays.asList(
                Command
                  .newBuilder()
                  .setType(CommandType.COMMAND_TYPE_INVALID)
                  .build()
              )
            )
          )
        )
        .build()
    );
  }

  private CommandsResponse openFile(EditorStateWithMetadata state) {
    List<CommandsResponseAlternative> alternatives = new ArrayList<>();
    for (int i = 0; i < state.getFilesList().size(); i++) {
      // can be removed once 1.10 propagates
      String path = state.getFilesList().get(i);
      int prefixLength = 0;
      for (String root : state.getRootsList()) {
        if (path.startsWith(root) && prefixLength < path.length()) {
          prefixLength = root.length() + 1;
        }
      }

      String displayPath = path.substring(prefixLength);
      alternatives.add(
        createAlternative(
          "open " + displayPath,
          "open <code>" + displayPath + "</code>",
          Arrays.asList(
            Command
              .newBuilder()
              .setType(CommandType.COMMAND_TYPE_OPEN_FILE)
              .setPath(displayPath)
              .setIndex(i)
              .build()
          )
        )
      );
    }

    if (alternatives.size() == 0) {
      throw new NoFileFound();
    }

    return CommandsResponse
      .newBuilder()
      .setTextResponse(true)
      .setFinal(true)
      .addAllAlternatives(alternatives)
      .build();
  }

  private CommandsResponse paste(
    EditorStateWithMetadata state,
    String directionName
  ) {
    NewlineNormalizer.Normalization normalization = newlineNormalizer.normalize(
      state
    );
    state = normalization.state;

    String text = state.getClipboard();
    String description =
      "paste" + (directionName.equals("") ? "" : " " + directionName);
    String source = state.getSource();
    int cursor = state.getCursor();
    InsertDirection direction;
    if (directionName.equals("above")) {
      direction = InsertDirection.ABOVE;
    } else if (directionName.equals("below")) {
      direction = InsertDirection.BELOW;
    } else if (directionName.equals("inline")) {
      direction = InsertDirection.INLINE;
    } else {
      direction = InsertDirection.NONE;
    }

    DiffWithMetadata diff = commandsFactory
      .create(state.getLanguage())
      .paste(source, cursor, text, direction);
    return CommandsResponse
      .newBuilder()
      .setTextResponse(true)
      .setExecute(
        normalization.revert(
          createAlternative(
            description,
            description,
            Arrays.asList(diff.toCommand())
          )
        )
      )
      .build();
  }

  public Optional<CommandsResponse> evaluate(
    CallbackRequest request,
    EditorStateWithMetadata state
  ) {
    try {
      if (request.getType() == CallbackType.CALLBACK_TYPE_CHAIN) {
        return Optional.of(
          CommandsResponse
            .newBuilder(
              transcriptEvaluator.evaluate(
                transcriptParser.parse(
                  Arrays.asList(
                    Alternative
                      .newBuilder()
                      .setTranscript(request.getText())
                      .build()
                  ),
                  state,
                  true
                ),
                state,
                true
              )
            )
            .clearAlternatives()
            .setTextResponse(true)
            .build()
        );
      } else if (request.getType() == CallbackType.CALLBACK_TYPE_OPEN_FILE) {
        return Optional.of(openFile(state));
      } else if (request.getType() == CallbackType.CALLBACK_TYPE_PASTE) {
        return Optional.of(paste(state, request.getText()));
      } else if (
        request.getType() == CallbackType.CALLBACK_TYPE_ADD_TO_HISTORY
      ) {
        history.add(state.getToken(), request.getText());
        return Optional.empty();
      } else if (
        request.getType() == CallbackType.CALLBACK_TYPE_CLEAR_HISTORY
      ) {
        history.clear(state.getToken());
        return Optional.empty();
      }
    } catch (Exception e) {
      return handleException(e);
    }

    return Optional.empty();
  }
}
