package core.evaluator;

import core.ast.AstFactory;
import core.ast.api.AstParent;
import core.codeengine.CodeEngineBatchQueue;
import core.exception.CannotDetermineLanguage;
import core.exception.CannotFindHistoryCommand;
import core.exception.CannotFindInsertionPoint;
import core.exception.SafeToDisplayException;
import core.formattedtext.FormattedTextConverter;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponse;
import core.gen.rpc.CommandsResponseAlternative;
import core.gen.rpc.CustomCommand;
import core.gen.rpc.ErrorCode;
import core.gen.rpc.Language;
import core.metadata.CommandsResponseAlternativeWithMetadata;
import core.metadata.EditorStateWithMetadata;
import core.parser.ParseTree;
import core.util.CommandLogger;
import core.util.PhraseHintExtractor;
import core.util.Whitespace;
import core.visitor.CommandsVisitor;
import core.visitor.CommandsVisitorContext;
import core.visitor.DescriptionVisitor;
import core.visitor.TreeConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolbelt.languages.LanguageDeterminer;
import toolbelt.logging.Logs;
import toolbelt.state.History;

@Singleton
public class TranscriptEvaluator {

  private Logger logger = LoggerFactory.getLogger(TranscriptEvaluator.class);

  private Set<CommandType> chainableCommands = Set.of(
    CommandType.COMMAND_TYPE_CANCEL,
    CommandType.COMMAND_TYPE_COPY,
    CommandType.COMMAND_TYPE_DIFF,
    CommandType.COMMAND_TYPE_USE
  );

  private Set<CommandType> executeCommandTypes = Set.of(
    CommandType.COMMAND_TYPE_CALLBACK,
    CommandType.COMMAND_TYPE_CANCEL,
    CommandType.COMMAND_TYPE_CLIPBOARD,
    CommandType.COMMAND_TYPE_COPY,
    CommandType.COMMAND_TYPE_DEBUGGER_CONTINUE,
    CommandType.COMMAND_TYPE_DEBUGGER_INLINE_BREAKPOINT,
    CommandType.COMMAND_TYPE_DEBUGGER_PAUSE,
    CommandType.COMMAND_TYPE_DEBUGGER_SHOW_HOVER,
    CommandType.COMMAND_TYPE_DEBUGGER_START,
    CommandType.COMMAND_TYPE_DEBUGGER_STEP_INTO,
    CommandType.COMMAND_TYPE_DEBUGGER_STEP_OUT,
    CommandType.COMMAND_TYPE_DEBUGGER_STEP_OVER,
    CommandType.COMMAND_TYPE_DEBUGGER_STOP,
    CommandType.COMMAND_TYPE_DEBUGGER_TOGGLE_BREAKPOINT,
    CommandType.COMMAND_TYPE_DIFF,
    CommandType.COMMAND_TYPE_HIDE_REVISION_BOX,
    CommandType.COMMAND_TYPE_INSERT,
    CommandType.COMMAND_TYPE_LANGUAGE_MODE,
    CommandType.COMMAND_TYPE_SCROLL,
    CommandType.COMMAND_TYPE_NEXT,
    CommandType.COMMAND_TYPE_PASTE,
    CommandType.COMMAND_TYPE_PAUSE,
    CommandType.COMMAND_TYPE_REDO,
    CommandType.COMMAND_TYPE_SAVE,
    CommandType.COMMAND_TYPE_SHOW,
    CommandType.COMMAND_TYPE_SHOW_REVISION_BOX,
    CommandType.COMMAND_TYPE_START_DICTATE,
    CommandType.COMMAND_TYPE_STOP_DICTATE,
    CommandType.COMMAND_TYPE_UNDO,
    CommandType.COMMAND_TYPE_USE
  );

  private Set<String> executeKeys = Set.of(
    "up",
    "down",
    "left",
    "right",
    "space",
    "enter",
    "tab",
    "pagedown",
    "pageup"
  );

  private final int maxAlternatives = 10;

  @Inject
  AstFactory astFactory;

  @Inject
  CommandLogger commandLogger;

  @Inject
  CommandsVisitor commandsVisitor;

  @Inject
  CustomCommandEvaluator customCommandEvaluator;

  @Inject
  History history;

  @Inject
  FormattedTextConverter formattedTextConverter;

  @Inject
  NewlineNormalizer newlineNormalizer;

  @Inject
  CodeEngineBatchQueue.Factory codeEngineBatchQueueFactory;

  @Inject
  Reranker reranker;

  @Inject
  TreeConverter treeConverter;

  @Inject
  Whitespace whitespace;

  @Inject
  public TranscriptEvaluator() {}

  // when enough clients have updated, we can remove this logic, because it lives on the client now
  private boolean canAutoExecute(CommandsResponseAlternative alternative) {
    // run commands are often in a terminal, where we don't want to do things unexpectedly
    if (alternative.getTranscript().startsWith("run")) {
      return false;
    }

    return alternative
      .getCommandsList()
      .stream()
      .allMatch(
        e ->
          executeCommandTypes.contains(e.getType()) ||
          (e.getType() == CommandType.COMMAND_TYPE_PRESS && executeKeys.contains(e.getText()))
      );
  }

  public List<CommandsResponseAlternative> capAlternatives(
    List<CommandsResponseAlternative> alternatives
  ) {
    List<CommandsResponseAlternative> ret = new ArrayList<>(
      alternatives
        .stream()
        .filter(a -> a.getCommandsList().get(0).getType() != CommandType.COMMAND_TYPE_INVALID)
        .limit(maxAlternatives)
        .collect(Collectors.toList())
    );
    List<CommandsResponseAlternative> invalidAlternatives = alternatives
      .stream()
      .filter(a -> a.getCommandsList().get(0).getType() == CommandType.COMMAND_TYPE_INVALID)
      .limit(maxAlternatives - ret.size())
      .collect(Collectors.toList());
    ret.addAll(invalidAlternatives);
    return ret;
  }

  private List<CommandsResponseAlternativeWithMetadata> evaluateTranscripts(
    List<ParsedTranscript> parsed,
    EditorStateWithMetadata state
  ) {
    Language language = state.getLanguage();
    CodeEngineBatchQueue queue = codeEngineBatchQueueFactory.create(language);

    List<CompletableFuture<List<CommandsResponseAlternativeWithMetadata>>> resolved = parsed
      .stream()
      .map(e -> evaluateTranscript(e, state, queue))
      .collect(Collectors.toList());

    queue.flush();
    List<CommandsResponseAlternativeWithMetadata> flattened = resolved
      .stream()
      .flatMap(e -> e.join().stream())
      .collect(Collectors.toList());

    List<CommandsResponseAlternativeWithMetadata> result = new ArrayList<>();
    Set<List<Command>> seen = new LinkedHashSet<>();
    for (CommandsResponseAlternativeWithMetadata alternativeWithMetadata : flattened) {
      CommandsResponseAlternative alternative = alternativeWithMetadata.toCommandsResponseAlternative();
      if (alternative.getCommands(0).getType() != CommandType.COMMAND_TYPE_INVALID) {
        if (seen.contains(alternative.getCommandsList())) {
          continue;
        }

        seen.add(alternative.getCommandsList());
      }

      result.add(alternativeWithMetadata);
    }

    return result;
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> evaluateTranscript(
    ParsedTranscript parsed,
    EditorStateWithMetadata state,
    CodeEngineBatchQueue queue
  ) {
    return customCommandEvaluator
      .evaluate(parsed, state, queue)
      .thenCompose(
        customAlternativeWithMetadata -> {
          if (customAlternativeWithMetadata.isPresent()) {
            return CompletableFuture.completedFuture(
              Arrays.asList(customAlternativeWithMetadata.get())
            );
          }

          if (!parsed.isValid) {
            return CompletableFuture.completedFuture(
              Arrays.asList(
                invalidAlternative(state, parsed, parsed.transcript(), ErrorCode.ERROR_CODE_NONE)
              )
            );
          }

          List<ParseTree> children = parsed.children();
          if (children == null || children.size() == 0) {
            return CompletableFuture.completedFuture(
              Arrays.asList(
                invalidAlternative(state, parsed, parsed.transcript(), ErrorCode.ERROR_CODE_NONE)
              )
            );
          }

          return startEvaluatingTranscript(queue, state, parsed)
            .thenApply(
              alternatives ->
                alternatives
                  .stream()
                  .map(
                    a ->
                      a.commands.size() == 0
                        ? handleException(
                          new RuntimeException("No valid commands"),
                          state,
                          parsed,
                          ErrorCode.ERROR_CODE_NONE
                        )
                        : a
                  )
                  .collect(Collectors.toList())
            );
        }
      )
      .exceptionally(
        e -> {
          return Arrays.asList(handleException(e, state, parsed, ErrorCode.ERROR_CODE_NONE));
        }
      );
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> continueEvaluatingTranscript(
    CodeEngineBatchQueue queue,
    CommandsResponseAlternativeWithMetadata evaluated,
    int nextIndex,
    int maxIndex
  ) {
    ParsedTranscript parsed = evaluated.parsed;
    EditorStateWithMetadata state = evaluated.finalState();
    Language language = state.getLanguage();
    List<ParseTree> children = parsed.children();
    if (nextIndex == maxIndex) {
      return CompletableFuture.completedFuture(Arrays.asList(evaluated));
    }

    if (!evaluated.commands.stream().allMatch(e -> chainableCommands.contains(e.getType()))) {
      CommandsResponseAlternativeWithMetadata result = new CommandsResponseAlternativeWithMetadata(
        evaluated
      );
      String remainingTranscript = parsed
        .transcript()
        .substring(children.get(nextIndex).getStart())
        .trim();
      result.remaining = Optional.of(remainingTranscript);
      // use the comma delimited transcript for the description of the remaining.
      result.description =
        Optional.of(
          Stream
            .concat(
              evaluated.description.stream(),
              children
                .stream()
                .skip(nextIndex)
                .map(c -> parsed.transcript().substring(c.getStart(), c.getStop()))
            )
            .collect(Collectors.joining(", "))
        );
      return CompletableFuture.completedFuture(Arrays.asList(result));
    }
    return evaluateCommand(parsed, children.get(nextIndex), state, queue)
      .thenCompose(
        alternatives -> {
          if (alternatives.size() == 0) {
            // Skip things like propositions, which are processed in other commands and don't return alternatives.
            alternatives =
              Arrays.asList(new CommandsResponseAlternativeWithMetadata(state, parsed));
          }
          List<CompletableFuture<List<CommandsResponseAlternativeWithMetadata>>> list = alternatives
            .stream()
            .map(
              alternative ->
                continueEvaluatingTranscript(queue, alternative, nextIndex + 1, maxIndex)
            )
            .collect(Collectors.toList());
          return CompletableFuture
            .allOf(list.toArray(new CompletableFuture[list.size()]))
            .thenApply(
              v -> list.stream().flatMap(e -> e.join().stream()).collect(Collectors.toList())
            );
        }
      )
      .thenApply(
        alternatives ->
          alternatives
            .stream()
            .map(
              alternative -> {
                CommandsResponseAlternativeWithMetadata result = new CommandsResponseAlternativeWithMetadata(
                  evaluated
                );

                result.commands.addAll(alternative.commands);
                result.remaining = alternative.remaining;
                result.description =
                  Optional
                    .of(
                      Stream
                        .concat(evaluated.description.stream(), alternative.description.stream())
                        .collect(Collectors.joining(", "))
                    )
                    .filter(s -> !s.equals(""));
                if (alternative.autoStyleCost.isPresent()) {
                  result.autoStyleCost = alternative.autoStyleCost;
                  result.contextualLanguageModelCost = alternative.contextualLanguageModelCost;
                  result.slotContext = alternative.slotContext;
                }
                if (alternative.errorCode.filter(e -> e != ErrorCode.ERROR_CODE_NONE).isPresent()) {
                  result.errorCode = alternative.errorCode;
                }
                return result;
              }
            )
            .collect(Collectors.toList())
      );
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> evaluateCommand(
    ParsedTranscript parsed,
    ParseTree child,
    EditorStateWithMetadata state,
    CodeEngineBatchQueue queue
  ) {
    DescriptionVisitor descriptionVisitor = new DescriptionVisitor(
      formattedTextConverter,
      treeConverter,
      whitespace,
      state.getLanguage()
    );
    return commandsVisitor
      .visit(child, new CommandsVisitorContext(state, parsed, queue, this))
      .thenApply(
        alternatives ->
          alternatives
            .stream()
            .map(
              alternative -> {
                if (alternative.description.isEmpty()) {
                  alternative.description = Optional.of(descriptionVisitor.visit(child, null));
                }

                return alternative;
              }
            )
            .collect(Collectors.toList())
      );
  }

  private Optional<Command> getMetaCommand(CommandsResponseAlternative alternative) {
    return alternative
      .getCommandsList()
      .stream()
      .filter(
        c ->
          c.getType() == CommandType.COMMAND_TYPE_CANCEL ||
          c.getType() == CommandType.COMMAND_TYPE_USE
      )
      .findFirst();
  }

  private CommandsResponseAlternativeWithMetadata handleException(
    Throwable e,
    EditorStateWithMetadata state,
    ParsedTranscript parsed,
    ErrorCode errorCode
  ) {
    String prefix = "An error occurred";
    if (e instanceof CompletionException) {
      e = e.getCause();
    }
    // This might be prone to errors that leak info to the user, so be careful.
    boolean displayTranscript = true;
    if (e instanceof SafeToDisplayException) {
      prefix = e.getMessage();
      displayTranscript = ((SafeToDisplayException) e).displayTranscript();
    } else {
      Logs.logError(
        logger,
        e.getMessage() != null ? e.getMessage() : "TranscriptEvaluator error",
        e
      );
    }

    return invalidAlternative(
      state,
      parsed,
      prefix + (displayTranscript ? ": " + parsed.transcript() : ""),
      errorCode
    );
  }

  private CommandsResponseAlternativeWithMetadata invalidAlternative(
    EditorStateWithMetadata state,
    ParsedTranscript parsed,
    String description,
    ErrorCode errorCode
  ) {
    // when transcripts update quickly, we don't want to show an error message for the transcript "type"
    // because it's likely just the start of a command
    if (parsed.transcript().equals("type")) {
      description = "type";
    }

    CommandsResponseAlternativeWithMetadata result = new CommandsResponseAlternativeWithMetadata(
      state,
      parsed,
      Arrays.asList(
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_INVALID)
          .setErrorCode(errorCode)
          .build()
      ),
      description
    );
    result.errorCode = Optional.of(errorCode);
    return result;
  }

  private CommandsResponse invalidateMetaCommandsIfNotFirst(CommandsResponse response) {
    return CommandsResponse
      .newBuilder(response)
      .clearAlternatives()
      .addAllAlternatives(
        Stream
          .concat(
            response.getAlternativesList().stream().limit(1),
            response
              .getAlternativesList()
              .stream()
              .skip(1)
              .map(
                alternative ->
                  getMetaCommand(alternative).isEmpty()
                    ? alternative
                    : CommandsResponseAlternative
                      .newBuilder(alternative)
                      .clearCommands()
                      .addAllCommands(
                        alternative
                          .getCommandsList()
                          .stream()
                          .map(
                            command ->
                              Command
                                .newBuilder(command)
                                .setType(CommandType.COMMAND_TYPE_INVALID)
                                .build()
                          )
                          .collect(Collectors.toList())
                      )
                      .build()
              )
          )
          .collect(Collectors.toList())
      )
      .build();
  }

  private CommandsResponse setExecuteToFirstAlternative(CommandsResponse response) {
    List<CommandsResponseAlternative> valid = response
      .getAlternativesList()
      .stream()
      .filter(
        e ->
          e.getCommandsList().size() > 0 &&
          e.getCommands(0).getType() != CommandType.COMMAND_TYPE_INVALID
      )
      .collect(Collectors.toList());

    if (valid.size() == 0) {
      return response;
    }

    // only auto-execute commands that are easy to undo
    if (valid.size() > 1 && !canAutoExecute(valid.get(0))) {
      return response;
    }

    return CommandsResponse
      .newBuilder(response)
      .setExecute(CommandsResponseAlternative.newBuilder(valid.get(0)).build())
      .build();
  }

  private CommandsResponse setCustomHintsLimitError(
    EditorStateWithMetadata state,
    CommandsResponse response
  ) {
    if (state.getCustomHints().size() > PhraseHintExtractor.customHintsLimit) {
      return CommandsResponse
        .newBuilder(response)
        .setSuggestion(
          String.format(
            "<p><b>Reached max hints in words.json. You have %d hints but the limit is %d.</b></p>",
            state.getCustomHints().size(),
            PhraseHintExtractor.customHintsLimit
          )
        )
        .build();
    }

    return response;
  }

  private CommandsResponse setSyntaxError(
    List<ParsedTranscript> parsed,
    EditorStateWithMetadata state,
    CommandsResponse response
  ) {
    // until we merge the MetadataResolver refactor, use a heuristic for commands that are
    // likely to trigger a parse, so we're not parsing files on insert commands, etc.
    if (
      !state.getPluginInstalled() ||
      !parsed
        .stream()
        .anyMatch(e -> e.transcript().startsWith("add") || e.transcript().startsWith("delete"))
    ) {
      return response;
    }

    Optional<String> syntaxErrorMessage = Optional.empty();
    try {
      AstParent sourceRoot = astFactory.createImmutableFileRoot(
        state.getSource(),
        state.getLanguage()
      );
      syntaxErrorMessage = sourceRoot.tree().getSyntaxError().map(e -> e.getMessage());
    } catch (CannotDetermineLanguage e) {} catch (Throwable e) {
      Logs.logError(logger, "Error checking for syntax error", e);
      return response;
    }

    if (syntaxErrorMessage.isPresent()) {
      return CommandsResponse
        .newBuilder(response)
        .setSuggestion(
          "<p><b>" +
          syntaxErrorMessage.get() +
          "!</b></p><p>Some commands may not behave as expected. You can say \"go to syntax error\" to find and resolve the error.</p>"
        )
        .build();
    }

    return response;
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> startEvaluatingTranscript(
    CodeEngineBatchQueue queue,
    EditorStateWithMetadata state,
    ParsedTranscript parsed
  ) {
    List<ParseTree> children = parsed.children();
    // Evaluating trailing prepositions first and produce the appropriate description.
    if (children.get(children.size() - 1).getType().equals("prepositionSelection")) {
      return evaluateCommand(parsed, children.get(children.size() - 1), state, queue)
        .thenCompose(
          prepositionAlternatives -> {
            List<CompletableFuture<List<CommandsResponseAlternativeWithMetadata>>> list = prepositionAlternatives
              .stream()
              .map(
                prepositionAlternative ->
                  continueEvaluatingTranscript(
                    queue,
                    new CommandsResponseAlternativeWithMetadata(
                      prepositionAlternative.finalState(),
                      parsed
                    ),
                    0,
                    children.size() - 1
                  )
                    .thenApply(
                      commandChainAlternatives -> {
                        return commandChainAlternatives
                          .stream()
                          .map(
                            commandChainAlternative -> {
                              CommandsResponseAlternativeWithMetadata result = new CommandsResponseAlternativeWithMetadata(
                                commandChainAlternative
                              );
                              result.commands.addAll(0, prepositionAlternative.commands);
                              result.description =
                                Optional.of(
                                  commandChainAlternative.description.get() +
                                  " " +
                                  prepositionAlternative.description.get()
                                );
                              if (
                                commandChainAlternative.errorCode
                                  .filter(e -> e != ErrorCode.ERROR_CODE_NONE)
                                  .isPresent()
                              ) {
                                result.errorCode = commandChainAlternative.errorCode;
                              }
                              return result;
                            }
                          )
                          .collect(Collectors.toList());
                      }
                    )
              )
              .collect(Collectors.toList());
            return CompletableFuture
              .allOf(list.toArray(new CompletableFuture[list.size()]))
              .thenApply(
                v -> list.stream().flatMap(e -> e.join().stream()).collect(Collectors.toList())
              );
          }
        );
    }

    return continueEvaluatingTranscript(
      queue,
      new CommandsResponseAlternativeWithMetadata(state, parsed),
      0,
      children.size()
    );
  }

  public CommandsResponse evaluate(
    List<ParsedTranscript> parsed,
    EditorStateWithMetadata state,
    boolean finalize
  ) {
    NewlineNormalizer.Normalization normalization = newlineNormalizer.normalize(state);
    List<CommandsResponseAlternative> alternatives = reranker
      .rerankEvaluated(evaluateTranscripts(parsed, normalization.state))
      .stream()
      .map(e -> normalization.revert(e.toCommandsResponseAlternative()))
      .collect(Collectors.toList());
    alternatives = capAlternatives(alternatives);

    CommandsResponse response = CommandsResponse
      .newBuilder()
      .addAllAlternatives(alternatives)
      .build();

    response = invalidateMetaCommandsIfNotFirst(response);
    response = setExecuteToFirstAlternative(response);
    if (finalize) {
      response = setSyntaxError(parsed, state, response);
      response = setCustomHintsLimitError(state, response);
    }

    return response;
  }
}
