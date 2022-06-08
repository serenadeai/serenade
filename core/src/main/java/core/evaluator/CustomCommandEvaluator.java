package core.evaluator;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import core.ast.Ast;
import core.ast.ScssAst;
import core.ast.api.DefaultAstParent;
import core.codeengine.CodeEngineBatchQueue;
import core.commands.Commands;
import core.commands.CommandsFactory;
import core.exception.TimeoutExceeded;
import core.formattedtext.FormattedTextConverter;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CustomCommand;
import core.gen.rpc.CustomCommandChainable;
import core.gen.rpc.CustomCommandOption;
import core.gen.rpc.Language;
import core.metadata.CommandsResponseAlternativeWithMetadata;
import core.metadata.DiffWithMetadata;
import core.metadata.EditorStateWithMetadata;
import core.snippet.Snippet;
import core.snippet.SnippetTrigger;
import core.snippet.Snippets;
import core.util.Range;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolbelt.languages.LanguageDeterminer;
import toolbelt.logging.Logs;

@Singleton
public class CustomCommandEvaluator {

  private class CustomCommandWithTranscript {

    private List<String> transcriptWords;
    private Range range;

    CustomCommand command;

    public CustomCommandWithTranscript(CustomCommand command, String transcript) {
      this.command = command;
      this.transcriptWords = Arrays.asList(transcript.split(" "));
      range = new Range(0, transcriptWords.size());
    }

    public CustomCommandWithTranscript(
      CustomCommand command,
      List<String> transcriptWords,
      Range range
    ) {
      this.command = command;
      this.transcriptWords = transcriptWords;
      this.range = range;
    }

    public String transcript() {
      return transcriptWords
        .subList(range.start, range.stop)
        .stream()
        .collect(Collectors.joining(" "));
    }
  }

  private SimpleTimeLimiter timeLimiter = SimpleTimeLimiter.create(Executors.newCachedThreadPool());
  private Cache<String, Pattern> matchingPatternCache = CacheBuilder
    .newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build();

  private Logger logger = LoggerFactory.getLogger(CustomCommandEvaluator.class);

  @Inject
  FormattedTextConverter formattedTextConverter;

  @Inject
  CommandsFactory commandsFactory;

  @Inject
  LanguageDeterminer languageDeterminer;

  @Inject
  Snippets.Factory snippetsFactory;

  @Inject
  Whitespace whitespace;

  @Inject
  public CustomCommandEvaluator() {}

  private List<String> commandProduction(CustomCommand command) {
    return Arrays
      .asList(command.getTemplated().split("\\s+"))
      .stream()
      .map(e -> SnippetTrigger.slotPattern.matcher(e).matches() ? "WILDCARD" : e)
      .collect(Collectors.toList());
  }

  private Optional<List<CustomCommandWithTranscript>> consumeCommand(
    Map<List<String>, CustomCommand> commands,
    List<String> transcriptWords,
    int commandStartIndex
  ) {
    if (commandStartIndex == transcriptWords.size()) {
      return Optional.of(new ArrayList<>());
    }

    return commands
      .entrySet()
      .stream()
      .flatMap(
        e ->
          consumeCommand(
            commands,
            transcriptWords,
            commandStartIndex,
            e.getKey(),
            e.getValue(),
            commandStartIndex,
            0,
            new ArrayList<>()
          )
            .stream()
      )
      .findFirst();
  }

  private Optional<List<CustomCommandWithTranscript>> consumeCommand(
    Map<List<String>, CustomCommand> commands,
    List<String> transcriptWords,
    int commandStartIndex,
    List<String> production,
    CustomCommand command,
    int consumed,
    int productionIndex,
    List<Range> productionMatches
  ) {
    if (
      productionIndex == 0 &&
      commandStartIndex > 0 &&
      command.getChainable() == CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_FIRST_ONLY
    ) {
      return Optional.empty();
    }
    if (Thread.interrupted()) {
      throw new RuntimeException(new InterruptedException());
    }

    while (
      consumed < transcriptWords.size() &&
      productionIndex < production.size() &&
      !production.get(productionIndex).equals("WILDCARD")
    ) {
      if (
        !transcriptWords
          .get(commandStartIndex + productionIndex)
          .equals(production.get(productionIndex))
      ) {
        return Optional.empty();
      }

      productionIndex++;
      consumed++;
    }

    // if we got to the end of the production without hitting a wildcard.
    List<Range> matches = new ArrayList<>();
    if (productionIndex == production.size()) {
      if (
        consumed < transcriptWords.size() &&
        command.getChainable() == CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_LAST_ONLY
      ) {
        return Optional.empty();
      }

      CustomCommandWithTranscript customCommandWithTranscript = new CustomCommandWithTranscript(
        command,
        transcriptWords,
        new Range(commandStartIndex, consumed)
      );

      return consumeCommand(commands, transcriptWords, consumed)
        .map(
          results -> {
            results.add(customCommandWithTranscript); // reversed later.
            return results;
          }
        );
    }

    // non-greedy match of wildcard fields
    Range match = new Range(consumed, consumed + 1);
    List<Range> updatedProductionMatches = new ArrayList<>(productionMatches);
    updatedProductionMatches.add(match);
    while (match.stop < transcriptWords.size() + 1) {
      Optional<List<CustomCommandWithTranscript>> remaining = consumeCommand(
        commands,
        transcriptWords,
        commandStartIndex,
        production,
        command,
        match.stop,
        productionIndex + 1,
        updatedProductionMatches
      );

      if (remaining.isPresent()) {
        return remaining;
      }

      match.stop++;
    }

    return Optional.empty();
  }

  private Optional<List<CustomCommandWithTranscript>> chainableCommandsForTranscript(
    String transcript,
    List<CustomCommand> commands
  ) {
    if (commands.size() == 0) {
      return Optional.empty();
    }

    Map<List<String>, CustomCommand> commandMap = commands
      .stream()
      .collect(Collectors.toMap(c -> commandProduction(c), c -> c));

    List<String> transcriptWords = Arrays.asList(transcript.split("\\s"));
    return consumeCommand(commandMap, transcriptWords, 0)
      .map(
        list -> {
          Collections.reverse(list);
          return list;
        }
      );
  }

  private Command evaluateApiCommand(
    CustomCommandWithTranscript command,
    EditorStateWithMetadata state
  ) {
    SnippetTrigger trigger = new SnippetTrigger(command.command.getTemplated());
    Map<String, List<FormattedTextOptions>> options = options(command.command);
    Map<String, String> slotValues = trigger.getSlotValuesFromTranscript(command.transcript());
    Map<String, String> replacements = new HashMap<>();
    for (String slot : slotValues.keySet()) {
      replacements.put(
        slot,
        formattedTextConverter.convert(
          slotValues.get(slot),
          options.get(slot) != null && !options.get(slot).isEmpty()
            ? options.get(slot).get(0)
            : FormattedTextOptions.newBuilder().build(),
          state.getLanguage()
        )
      );
    }

    return Command
      .newBuilder()
      .setType(CommandType.COMMAND_TYPE_CUSTOM)
      .setCustomCommandId(command.command.getId())
      .putAllReplacements(replacements)
      .build();
  }

  private CompletableFuture<Optional<CommandsResponseAlternativeWithMetadata>> evaluateSnippetCommand(
    ParsedTranscript parsed,
    Commands commands,
    EditorStateWithMetadata state,
    SnippetTrigger trigger,
    CustomCommand command,
    Language language,
    CodeEngineBatchQueue queue
  ) {
    Map<String, List<FormattedTextOptions>> options = options(command);
    Snippets snippets = snippetsFactory.create(language);
    String snippetType = command.getSnippetType();
    Snippets.GenerateTemplatedCode generate = (parent, matches) -> command.getGenerated();
    Snippet snippet;
    if (language == Language.LANGUAGE_DEFAULT) {
      return snippets
        .insert(trigger, generate, options, false)
        .apply(
          state.getSource(),
          state.getCursor(),
          new DefaultAstParent(),
          parsed.transcript(),
          queue
        )
        .thenApply(
          diffs ->
            Optional.of(
              new CommandsResponseAlternativeWithMetadata(
                state,
                parsed,
                diffs.get(0),
                parsed.transcript()
              )
            )
        );
    }

    if (snippetType.equals("inline")) {
      snippet = snippets.insert(trigger, generate, options, false);
    } else if (snippetType.equals("argument")) {
      snippet = snippets.addToList(trigger, Ast.Argument.class, generate, options);
    } else if (snippetType.equals("attribute")) {
      snippet = snippets.addToList(trigger, Ast.MarkupAttribute.class, generate, options);
    } else if (snippetType.equals("catch")) {
      snippet = snippets.addToOptional(trigger, Ast.CatchClause.class, generate, options);
    } else if (snippetType.equals("class")) {
      snippet =
        language == Language.LANGUAGE_PYTHON
          ? snippets.addToTopLevelStatementList(trigger, Ast.Class_.class, generate, options)
          : snippets.addToList(trigger, Ast.Class_.class, generate, options);
    } else if (snippetType.equals("decorator")) {
      snippet = snippets.addToList(trigger, Ast.Decorator.class, generate, options);
    } else if (snippetType.equals("element")) {
      snippet = snippets.addToList(trigger, Ast.ListElement.class, generate, options);
    } else if (snippetType.equals("else")) {
      snippet = snippets.addToOptional(trigger, Ast.ElseClause.class, generate, options);
    } else if (snippetType.equals("else_if")) {
      snippet = snippets.addToList(trigger, Ast.ElseIfClause.class, generate, options);
    } else if (snippetType.equals("entry")) {
      snippet = snippets.addToList(trigger, Ast.KeyValuePair.class, generate, options);
    } else if (snippetType.equals("enum")) {
      snippet = snippets.addToList(trigger, Ast.Enum.class, generate, options);
    } else if (snippetType.equals("finally")) {
      snippet = snippets.addToOptional(trigger, Ast.FinallyClause.class, generate, options);
    } else if (snippetType.equals("function")) {
      snippet =
        language == Language.LANGUAGE_PYTHON
          ? snippets.addToTopLevelStatementList(trigger, Ast.Function.class, generate, options)
          : snippets.addToList(trigger, Ast.Function.class, generate, options);
    } else if (snippetType.equals("import")) {
      snippet = snippets.addToList(trigger, Ast.Import.class, generate, options);
    } else if (snippetType.equals("method")) {
      snippet = snippets.addToList(trigger, Ast.Method.class, generate, options);
    } else if (snippetType.equals("parameter")) {
      snippet = snippets.addToList(trigger, Ast.Parameter.class, generate, options);
    } else if (snippetType.equals("ruleset")) {
      snippet = snippets.addToList(trigger, Ast.CssRuleset.class, generate, options);
    } else if (snippetType.equals("return_value")) {
      snippet = snippets.addToOptional(trigger, Ast.ReturnValue.class, generate, options);
    } else if (snippetType.equals("tag")) {
      snippet = snippets.addToList(trigger, Ast.MarkupElement.class, generate, options);
    } else if (language == Language.LANGUAGE_HTML || language == Language.LANGUAGE_SCSS) {
      snippet = snippets.insert(trigger, generate, options, true);
    } else {
      snippet = snippets.addToList(trigger, Ast.Statement.class, generate, options);
    }

    snippet.internal = false;
    return commands
      .add(state.getSource(), state.getCursor(), parsed.transcript(), snippet, queue)
      .thenApply(
        diffs ->
          Optional.of(
            new CommandsResponseAlternativeWithMetadata(
              state,
              parsed,
              diffs.get(0),
              parsed.transcript()
            )
          )
      );
  }

  // this can be removed when 1.10 propagates
  private boolean matchesState(EditorStateWithMetadata state, CustomCommand command) {
    return (
      (
        command
          .getApplicationsList()
          .stream()
          .anyMatch(e -> state.getApplication().toLowerCase().contains(e.toLowerCase())) ||
        command.getApplicationsList().size() == 0
      ) &&
      (
        command
          .getLanguagesList()
          .stream()
          .anyMatch(e -> languageDeterminer.apiNames(state.getLanguage()).contains(e)) ||
        command.getLanguagesList().size() == 0
      ) &&
      (
        command.getExtensionsList().stream().anyMatch(e -> state.getFilename().endsWith(e)) ||
        command.getExtensionsList().size() == 0
      ) &&
      (
        command.getUrlsList().size() == 0 ||
        command.getUrlsList().stream().anyMatch(e -> state.getUrl().contains(e))
      )
    );
  }

  private Map<String, List<FormattedTextOptions>> options(CustomCommand command) {
    Map<String, List<FormattedTextOptions>> options = new HashMap<>();
    options.putAll(optionsFromString(command.getTemplated()));
    options.putAll(optionsFromString(command.getGenerated()));
    for (CustomCommandOption option : command.getOptionsList()) {
      options.put(
        option.getSlot(),
        Arrays.asList(FormattedTextOptions.fromString(option.getOptionsList()))
      );
    }

    return options;
  }

  private Map<String, List<FormattedTextOptions>> optionsFromString(String string) {
    Map<String, List<FormattedTextOptions>> result = new HashMap<>();
    Matcher match = Pattern.compile("<%\\s*(.+?)\\s*%>").matcher(string);
    while (match.find()) {
      List<String> split = Arrays.asList(match.group(1).trim().split(":"));
      if (split.size() > 1) {
        result.put(
          split.get(0),
          Arrays.asList(
            FormattedTextOptions.fromString(split.stream().skip(1).collect(Collectors.toList()))
          )
        );
      }
    }

    return result;
  }

  private Optional<CustomCommandWithTranscript> unchainableCommandsForTranscript(
    String transcript,
    List<CustomCommand> commands
  ) {
    if (commands.size() == 0) {
      return Optional.empty();
    }

    String pattern =
      "^(?:" +
      commands
        .stream()
        .map(
          command ->
            "(" +
            SnippetTrigger.slotPattern
              .matcher(command.getTemplated())
              .replaceAll(SnippetTrigger.wildcardPattern) +
            ")"
        )
        .collect(Collectors.joining("|")) +
      ")$";

    Matcher match;
    try {
      match = matchingPatternCache.get(pattern, () -> Pattern.compile(pattern)).matcher(transcript);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }

    if (match.find()) {
      for (int i = 0; i < match.groupCount(); i++) {
        if (match.group(i + 1) != null) {
          return Optional.of(new CustomCommandWithTranscript(commands.get(i), transcript));
        }
      }
    }

    return Optional.empty();
  }

  public CompletableFuture<Optional<CommandsResponseAlternativeWithMetadata>> evaluate(
    ParsedTranscript parsed,
    EditorStateWithMetadata state,
    CodeEngineBatchQueue queue
  ) {
    try {
      return timeLimiter.callWithTimeout(
        () -> {
          Language language = state.getLanguage();
          Commands commands = commandsFactory.create(language);

          List<CustomCommand> customCommands = new ArrayList<>(state.getCustomCommandsList());
          Collections.sort(
            customCommands,
            (a, b) ->
              SnippetTrigger.slotPattern.matcher(b.getTemplated()).replaceAll("_").length() -
              SnippetTrigger.slotPattern.matcher(a.getTemplated()).replaceAll("_").length()
          );

          Optional<CustomCommandWithTranscript> unchainableCommands = unchainableCommandsForTranscript(
            parsed.transcript(),
            customCommands
              .stream()
              .filter(
                command ->
                  matchesState(state, command) &&
                  command.getChainable() == CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_NONE
              )
              .collect(Collectors.toList())
          );

          Optional<List<CustomCommandWithTranscript>> chainableCommands = chainableCommandsForTranscript(
            parsed.transcript(),
            customCommands
              .stream()
              .filter(
                command ->
                  matchesState(state, command) &&
                  command.getChainable() != CustomCommandChainable.CUSTOM_COMMAND_CHAINABLE_NONE
              )
              .collect(Collectors.toList())
          );

          List<CustomCommandWithTranscript> result = unchainableCommands
            .map(e -> Arrays.asList(e))
            .orElse(chainableCommands.orElse(Arrays.asList()));

          // for now, we only support one snippet command at a time
          if (
            result.size() == 0 ||
            (
              result.size() > 1 &&
              result
                .stream()
                .filter(command -> !command.command.getGenerated().equals(""))
                .count() >
              0
            )
          ) {
            return CompletableFuture.completedFuture(Optional.empty());
          }

          if (!result.get(0).command.getGenerated().equals("")) {
            return evaluateSnippetCommand(
              parsed,
              commands,
              state,
              new SnippetTrigger(result.get(0).command.getTemplated()),
              result.get(0).command,
              language,
              queue
            );
          }

          return CompletableFuture.completedFuture(
            Optional.of(
              new CommandsResponseAlternativeWithMetadata(
                state,
                parsed,
                result
                  .stream()
                  .map(command -> evaluateApiCommand(command, state))
                  .collect(Collectors.toList()),
                parsed.transcript()
              )
            )
          );
        },
        100,
        TimeUnit.MILLISECONDS
      );
    } catch (Exception e) {
      Logs.logError(logger, "Custom command evaluate exception", e);
      throw new TimeoutExceeded();
    }
  }

  public Optional<SnippetTrigger> trigger(String transcript, EditorStateWithMetadata state) {
    for (CustomCommand command : state.getCustomCommandsList()) {
      SnippetTrigger trigger = new SnippetTrigger(command.getTemplated());
      if (matchesState(state, command) && trigger.matches(transcript)) {
        return Optional.of(trigger);
      }
    }

    return Optional.empty();
  }
}
