package core.visitor;

import core.commands.Commands;
import core.commands.CommandsFactory;
import core.commands.Styler;
import core.evaluator.TranscriptParser;
import core.exception.CannotFindHistoryCommand;
import core.exception.NoFileFound;
import core.exception.ObjectNotFound;
import core.formattedtext.FormattedTextConverter;
import core.formattedtext.FormattedTextOptions;
import core.formattedtext.TextConversionMap;
import core.gen.rpc.CallbackType;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponse;
import core.gen.rpc.CustomCommand;
import core.gen.rpc.ErrorCode;
import core.gen.rpc.Language;
import core.gen.rpc.StylerType;
import core.metadata.CommandsResponseAlternativeWithMetadata;
import core.metadata.DiffWithMetadata;
import core.metadata.EditorStateWithMetadata;
import core.parser.ParseTree;
import core.snippet.SnippetCollectionFactory;
import core.util.ArrowKeyDirection;
import core.util.Diff;
import core.util.InsertDirection;
import core.util.ObjectType;
import core.util.Range;
import core.util.SearchDirection;
import core.util.TextStyle;
import core.util.Whitespace;
import core.util.selection.Selection;
import core.util.selection.SelectionEndpoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import speechengine.gen.rpc.Alternative;
import toolbelt.languages.LanguageDeterminer;
import toolbelt.state.History;

@Singleton
public class CommandsVisitor
  extends BaseVisitor<CommandsVisitorContext, CompletableFuture<List<CommandsResponseAlternativeWithMetadata>>> {

  protected TreeConverter treeConverter;
  protected History history;
  protected LanguageDeterminer languageDeterminer;
  protected KeyConverter keyConverter;
  protected CommandsFactory commandsFactory;
  protected SnippetCollectionFactory snippetCollectionFactory;
  protected FormattedTextConverter formattedTextConverter;
  protected TranscriptParser transcriptParser;
  protected Whitespace whitespace;
  protected Styler styler;
  protected TextConversionMap textConversionMap;

  @Inject
  public CommandsVisitor(
    History history,
    LanguageDeterminer languageDeterminer,
    TranscriptParser transcriptParser,
    TreeConverter treeConverter,
    KeyConverter keyConverter,
    CommandsFactory commandsFactory,
    SnippetCollectionFactory snippetCollectionFactory,
    FormattedTextConverter formattedTextConverter,
    Whitespace whitespace,
    Styler styler,
    TextConversionMap textConversionMap
  ) {
    this.languageDeterminer = languageDeterminer;
    this.history = history;
    this.treeConverter = treeConverter;
    this.keyConverter = keyConverter;
    this.commandsFactory = commandsFactory;
    this.snippetCollectionFactory = snippetCollectionFactory;
    this.formattedTextConverter = formattedTextConverter;
    this.transcriptParser = transcriptParser;
    this.whitespace = whitespace;
    this.styler = styler;
    this.textConversionMap = textConversionMap;

    register("add", this::visitAdd);
    register("arrowKeyWithQuantifier", this::visitArrowKeyWithQuantifier);
    register("autocomplete", this::visitAutocomplete);
    register("back", this::visitBack);
    register("bareCopy", this::visitBareCopy);
    register("bareCut", this::visitBareCut);
    register("bareInspect", this::visitBareInspect);
    register("cancel", this::visitCancel);
    register("close", this::visitClose);
    register("change", this::visitChange);
    register("changeAll", this::visitChangeAll);
    register("click", this::visitClick);
    register("command", this::visitCommand);
    register("comment", this::visitComment);
    register("copy", this::visitCopy);
    register("cut", this::visitCut);
    register("debug", this::visitDebug);
    register("delete", this::visitDelete);
    register("dictate", this::visitDictate);
    register("dictateStart", this::visitDictateStart);
    register("dictateStop", this::visitDictateStop);
    register("duplicate", this::visitDuplicate);
    register("edit", this::visitEdit);
    register("focus", this::visitFocus);
    register("goTo", this::visitGoTo);
    register("goToDefinition", this::visitGoToDefinition);
    register("goToPhrase", this::visitGoToPhrase);
    register("goToSyntaxError", this::visitGoToSyntaxError);
    register("forward", this::visitForward);
    register("indent", this::visitIndent);
    register("insert", this::visitInsert);
    register("launch", this::visitLaunch);
    register("inspect", this::visitInspect);
    register("joinLines", this::visitJoinLines);
    register("languageMode", this::visitLanguageMode);
    register("newline", this::visitNewline);
    register("next", this::visitNext);
    register("openFile", this::visitOpenFile);
    register("paste", this::visitPaste);
    register("pause", this::visitPause);
    register("prepositionSelection", this::visitPrepositionSelection);
    register("press", this::visitPress);
    register("redo", this::visitRedo);
    register("reload", this::visitReload);
    register("rename", this::visitRename);
    register("repeat", this::visitRepeat);
    register("run", this::visitRun);
    register("save", this::visitSave);
    register("scroll", this::visitScroll);
    register("scrollPhrase", this::visitScrollPhrase);
    register("select", this::visitSelect);
    register("send", this::visitSend);
    register("shift", this::visitShift);
    register("show", this::visitShow);
    register("showDictationBox", this::visitShowDictationBox);
    register("sort", this::visitSort);
    register("split", this::visitSplit);
    register("style", this::visitStyle);
    register("setTextStyle", this::visitSetTextStyle);
    register("surroundWith", this::visitSurroundWith);
    register("switchWindow", this::visitSwitchWindow);
    register("systemInsert", this::visitSystemInsert);
    register("tabs", this::visitTabs);
    register("type", this::visitType);
    register("undo", this::visitUndo);
    register("undoCloseTab", this::visitUndoCloseTab);
    register("use", this::visitUse);
    register("window", this::visitWindow);
  }

  private boolean canGetState(CommandsVisitorContext context) {
    if (context.state.getClientIdentifier().contains("1.8.")) {
      return context.state.getPluginInstalled() || context.state.getCanGetState();
    }

    return context.state.getCanGetState();
  }

  private boolean canSetState(CommandsVisitorContext context) {
    if (context.state.getClientIdentifier().contains("1.8.")) {
      return context.state.getPluginInstalled() || context.state.getCanSetState();
    }

    return context.state.getCanSetState();
  }

  private String convertFormattedTextNode(ParseTree node, CommandsVisitorContext context) {
    return formattedTextConverter.convert(
      treeConverter.convertToEnglish(node.getChild("formattedText").get()),
      FormattedTextOptions.newBuilder().build(),
      context.state.getLanguage()
    );
  }

  private DescriptionVisitor createDescriptionVisitor(EditorStateWithMetadata state) {
    return new DescriptionVisitor(
      formattedTextConverter,
      treeConverter,
      whitespace,
      state.getLanguage()
    );
  }

  private String description(
    EditorStateWithMetadata state,
    ParseTree parent,
    ParseTree node,
    String code
  ) {
    DescriptionVisitor descriptionVisitor = createDescriptionVisitor(state);
    descriptionVisitor.setCodeNode(node, code);
    return descriptionVisitor.visit(parent, null);
  }

  private String descriptionForReplacement(
    EditorStateWithMetadata state,
    ParseTree node,
    Selection selection,
    ParseTree selectionNode,
    DiffWithMetadata diff
  ) {
    DescriptionVisitor descriptionVisitor = createDescriptionVisitor(state);
    selectionNameTree(selectionNode)
      .ifPresent(
        nameNode -> {
          String name = selection.object == ObjectType.PHRASE
            ? diff.codeToBeReplacedForDescription.get()
            : selection.name.get();
          descriptionVisitor.setCodeNode(nameNode, name);
        }
      );

    ParseTree textNode = node.getChild("formattedText").get();
    descriptionVisitor.setCodeNode(textNode, diff.codeForDescription.orElse(""));
    return descriptionVisitor.visit(node, null);
  }

  private List<String> commandOrControl(CommandsVisitorContext context) {
    return Arrays.asList(isMac(context) ? "command" : "control");
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> commandWithFormattedText(
    CommandsVisitorContext context,
    CommandType type,
    ParseTree node,
    FormattedTextOptions options,
    boolean invalidIfShort
  ) {
    ParseTree formattedTextNode = node.getChild("formattedText").get();
    String english = treeConverter.convertToEnglish(formattedTextNode);
    if (invalidIfShort && english.length() < 3) {
      return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_INVALID).build());
    }

    String text = formattedTextConverter.convert(english, options, context.state.getLanguage());
    return CompletableFuture.completedFuture(
      Arrays.asList(
        new CommandsResponseAlternativeWithMetadata(
          context.state,
          context.parsed,
          Arrays.asList(Command.newBuilder().setType(type).setText(text).build()),
          description(context.state, node, formattedTextNode, text)
        )
      )
    );
  }

  private Optional<Command> getAutocompleteTrigger(CommandsVisitorContext context) {
    // in atom, pressing control+space will automatically execute the first suggestion, which we
    // don't want, so only enable this for vscode
    if (
      context.state.getPluginInstalled() &&
      context.state.getAutocomplete() &&
      context.state.getApplication().equals("vscode")
    ) {
      return Optional.of(
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(Arrays.asList("control"))
          .setText("space")
          .build()
      );
    }

    return Optional.empty();
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> insert(
    CommandsVisitorContext context,
    InsertDirection direction,
    boolean enableCodeEngine,
    ParseTree formattedTextNode,
    ParseTree node
  ) {
    FormattedTextOptions options = FormattedTextOptions.newBuilder().build();

    // if code engine is enabled, then we're probably writing code, so set the options to
    // a camel case expression as a fallback in case the code engine disallow list triggers
    if (enableCodeEngine) {
      options =
        FormattedTextOptions
          .newBuilder()
          .setExpression(true)
          .setStyle(TextStyle.CAMEL_CASE)
          .build();
    }

    String english = treeConverter.convertToEnglish(formattedTextNode);
    Optional<Command> autocomplete = getAutocompleteTrigger(context);
    return commandsFactory
      .create(context.state.getLanguage())
      .insert(
        context.state.getSource(),
        context.state.getCursor(),
        english,
        direction,
        enableCodeEngine,
        Optional.of(options),
        context.queue
      )
      .thenApply(
        diffs ->
          diffs
            .stream()
            .map(
              diff -> {
                CommandsResponseAlternativeWithMetadata result = new CommandsResponseAlternativeWithMetadata(
                  context.state,
                  context.parsed,
                  diff,
                  description(
                    context.state,
                    node,
                    formattedTextNode,
                    diff.codeForDescription.orElse("")
                  )
                );

                if (autocomplete.isPresent()) {
                  result.commands.add(autocomplete.get());
                }

                return result;
              }
            )
            .collect(Collectors.toList())
      );
  }

  private boolean isApp(CommandsVisitorContext context, String app) {
    return context.state.getApplication().contains(app);
  }

  private boolean isBrowser(CommandsVisitorContext context) {
    return (
      isApp(context, "chrome") ||
      isApp(context, "firefox") ||
      isApp(context, "safari") ||
      isApp(context, "brave") ||
      isApp(context, "edge")
    );
  }

  private boolean isMac(CommandsVisitorContext context) {
    return context.state.getClientIdentifier().contains("darwin");
  }

  private boolean isRevisionBox(CommandsVisitorContext context) {
    if (context.state.getClientIdentifier().contains("1.8.")) {
      return context.state.getApplication().equals("dictation");
    }

    return context.state.getApplication().equals("revision-box");
  }

  private boolean isTerminal(CommandsVisitorContext context) {
    return isApp(context, "hyper") || isApp(context, "term");
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> navigateTo(
    CommandsVisitorContext context,
    ParseTree node,
    ParseTree destinationNode
  ) {
    String english = treeConverter.convertToEnglish(destinationNode);
    if (english.length() < 3) {
      return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_INVALID).build());
    }

    String url = formattedTextConverter.convert(
      english,
      FormattedTextOptions.newBuilder().setExpression(false).build(),
      Language.LANGUAGE_DEFAULT
    );

    if (url.contains(".") || url.contains("/")) {
      url = url.replaceAll(" ", "");
    }

    return CompletableFuture.completedFuture(
      Arrays.asList(
        new CommandsResponseAlternativeWithMetadata(
          context.state,
          context.parsed,
          Arrays.asList(
            Command
              .newBuilder()
              .setType(CommandType.COMMAND_TYPE_PRESS)
              .addAllModifiers(commandOrControl(context))
              .setText("l")
              .build(),
            Command.newBuilder().setType(CommandType.COMMAND_TYPE_INSERT).setText(url).build(),
            Command.newBuilder().setType(CommandType.COMMAND_TYPE_PRESS).setText("enter").build()
          ),
          description(context.state, node, destinationNode, url)
        )
      )
    );
  }

  private CompletableFuture<CommandsResponseAlternativeWithMetadata> repeat(
    Function<CommandsVisitorContext, CompletableFuture<List<CommandsResponseAlternativeWithMetadata>>> command,
    CommandsVisitorContext context,
    int count
  ) {
    if (count == 0) {
      return CompletableFuture.completedFuture(
        new CommandsResponseAlternativeWithMetadata(context.state, context.parsed)
      );
    }

    return command
      .apply(context)
      .thenCompose(
        alternatives -> {
          // we assume there's only one interpretation of quantified commands to avoid
          // exponential
          // blowup.
          CommandsResponseAlternativeWithMetadata alternative = alternatives.get(0);
          context.state = alternative.finalState();
          return repeat(command, context, count - 1)
            .thenApply(
              remaining -> {
                alternative.commands = new ArrayList<>(alternative.commands);
                alternative.commands.addAll(remaining.commands);
                return alternative;
              }
            );
        }
      );
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> repeatCommand(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    List<String> transcripts = history.all(context.state.getToken());
    String search = "";
    if (node.getChild("formattedText").isPresent()) {
      search = treeConverter.convertToEnglish(node.getChild("formattedText").get());
    }

    // handle histories like "insert foo", "repeat insert", "repeat", "repeat".
    if (search.equals("")) {
      // skip "repeat" commands immediately before this.
      int i = 0;
      while (
        i < transcripts.size() &&
        (transcripts.get(i).equals("repeat") || transcripts.get(i).equals("again"))
      ) {
        i++;
      }
      transcripts = transcripts.subList(i, transcripts.size());

      // if you're repeating "repeat x", then search for x again.
      if (transcripts.size() > 0 && transcripts.get(0).startsWith("repeat ")) {
        search = transcripts.get(0).substring("repeat ".length());
        transcripts = transcripts.subList(1, transcripts.size());
      }
    }

    for (String transcript : transcripts) {
      if (
        !transcript.equals("") &&
        transcript.contains(search) &&
        !(transcript.contains("repeat") || transcript.contains("again"))
      ) {
        CommandsResponse evaluated = context.transcriptEvaluator.evaluate(
          transcriptParser.parse(
            Arrays.asList(Alternative.newBuilder().setTranscript(transcript).build()),
            context.state,
            true
          ),
          context.state,
          true
        );

        if (evaluated.getAlternativesList().size() == 0) {
          continue;
        }

        List<Command> commands = evaluated
          .getAlternatives(0)
          .getCommandsList()
          .stream()
          .filter(e -> e.getType() != CommandType.COMMAND_TYPE_USE)
          .collect(Collectors.toList());

        if (commands.size() == 0) {
          continue;
        }

        CommandsResponseAlternativeWithMetadata result = new CommandsResponseAlternativeWithMetadata(
          context.state,
          context.parsed,
          commands
        );
        result.description = Optional.of(evaluated.getAlternatives(0).getDescription());
        return CompletableFuture.completedFuture(Arrays.asList(result));
      }
    }

    throw new CannotFindHistoryCommand();
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> requiresPlugin(
    CommandsVisitorContext context
  ) {
    return CompletableFuture.completedFuture(
      Arrays.asList(
        new CommandsResponseAlternativeWithMetadata(
          context.state,
          context.parsed,
          Arrays.asList(
            Command
              .newBuilder()
              .setType(CommandType.COMMAND_TYPE_INVALID)
              .setErrorCode(ErrorCode.ERROR_CODE_REQUIRES_PLUGIN)
              .build()
          ),
          "Requires plugin: " + context.parsed.transcript()
        )
      )
    );
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> requiresSource(
    CommandsVisitorContext context
  ) {
    return CompletableFuture.completedFuture(
      Arrays.asList(
        new CommandsResponseAlternativeWithMetadata(
          context.state,
          context.parsed,
          Arrays.asList(
            Command
              .newBuilder()
              .setType(CommandType.COMMAND_TYPE_INVALID)
              .setErrorCode(ErrorCode.ERROR_CODE_REQUIRES_SOURCE)
              .build()
          ),
          "No source, try \"revise\": " + context.parsed.transcript()
        )
      )
    );
  }

  private Optional<ParseTree> selectionNameTree(ParseTree selectionNode) {
    if (selectionNode.getType().equals("formattedText")) {
      return Optional.of(selectionNode);
    }
    for (ParseTree child : selectionNode.getChildren()) {
      Optional<ParseTree> ret = selectionNameTree(child);
      if (ret.isPresent()) {
        return ret;
      }
    }
    return Optional.empty();
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> wrap(
    CommandsVisitorContext context,
    Command command
  ) {
    return CompletableFuture.completedFuture(
      Arrays.asList(
        new CommandsResponseAlternativeWithMetadata(
          context.state,
          context.parsed,
          Arrays.asList(command)
        )
      )
    );
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> wrap(
    CommandsVisitorContext context,
    List<Command> commands
  ) {
    return CompletableFuture.completedFuture(
      Arrays.asList(
        new CommandsResponseAlternativeWithMetadata(context.state, context.parsed, commands)
      )
    );
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> wrap(
    CommandsVisitorContext context,
    Diff diff
  ) {
    return CompletableFuture.completedFuture(
      Arrays.asList(
        new CommandsResponseAlternativeWithMetadata(
          context.state,
          context.parsed,
          new DiffWithMetadata(diff)
        )
      )
    );
  }

  private CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> wrap(
    CommandsVisitorContext context,
    DiffWithMetadata diff
  ) {
    return CompletableFuture.completedFuture(
      Arrays.asList(
        new CommandsResponseAlternativeWithMetadata(context.state, context.parsed, diff)
      )
    );
  }

  @Override
  protected CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> aggregateResult(
    CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> aggregate,
    CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> next
  ) {
    return next;
  }

  @Override
  protected CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> defaultResult() {
    return CompletableFuture.completedFuture(Arrays.asList());
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitAdd(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    ParseTree textNode = node.getChild("formattedText").get();
    if (isTerminal(context) || !canSetState(context)) {
      return insert(context, InsertDirection.NONE, true, textNode, node);
    }

    String transcript = treeConverter.convertToEnglish(textNode);
    Optional<Command> autocomplete = getAutocompleteTrigger(context);
    return commandsFactory
      .create(context.state.getLanguage())
      .add(context.state.getSource(), context.state.getCursor(), transcript, context.queue)
      .thenApply(
        diffs ->
          diffs
            .stream()
            .map(
              diff -> {
                String prefix = "";
                if (diff.nameForDescription.isPresent()) {
                  prefix = diff.nameForDescription.get() + " ";
                }

                DescriptionVisitor descriptionVisitor = createDescriptionVisitor(context.state);
                descriptionVisitor.setCodeNode(
                  textNode,
                  prefix,
                  diff.codeForDescription.orElse("")
                );

                CommandsResponseAlternativeWithMetadata result = new CommandsResponseAlternativeWithMetadata(
                  context.state,
                  context.parsed,
                  diff,
                  descriptionVisitor.visit(node, null)
                );

                if (autocomplete.isPresent()) {
                  result.commands.add(autocomplete.get());
                }

                return result;
              }
            )
            .collect(Collectors.toList())
      );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitArrowKeyWithQuantifier(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    String direction = treeConverter.convertToDirectionString(node).orElse("down");
    int quantifier = 1;
    if (node.getChild("quantifier").isPresent()) {
      quantifier = treeConverter.convertQuantifier(node.getChild("quantifier").get());
    } else if (node.getChild("numberRange1To10").isPresent()) {
      quantifier = treeConverter.convertNumber(node.getChild("numberRange1To10").get());
    }

    List<Command> commands = new ArrayList<>();
    commands.add(
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_PRESS)
        .setText(direction)
        .setIndex(quantifier)
        .build()
    );

    // in a plugin-supported editor, we can make undo + use commands work with system presses by
    // prefixing the response with the current editor state so that the previous source + cursor
    // is restored when one of these commands is selected
    if (context.state.getPluginInstalled()) {
      commands.add(
        0,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_DIFF)
          .setSource(context.state.getSource())
          .setCursor(context.state.getCursor())
          .build()
      );
    }

    return wrap(context, commands);
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitAutocomplete(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_PRESS)
        .addAllModifiers(Arrays.asList("control"))
        .setText("space")
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitBack(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    // in NUX mode, "back" controls nux
    if (context.state.getNux()) {
      return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_BACK).build());
    }

    if (isBrowser(context)) {
      if (context.state.getPluginInstalled()) {
        return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_BACK).build());
      }

      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(Arrays.asList(isMac(context) ? "command" : "alt"))
          .setText("left")
          .build()
      );
    }

    if (context.state.getPluginInstalled()) {
      return wrap(
        context,
        commandsFactory
          .create(context.state.getLanguage())
          .goInDirection(
            context.state.getSource(),
            context.state.getCursor(),
            ArrowKeyDirection.LEFT
          )
      );
    }

    return wrap(
      context,
      Command.newBuilder().setType(CommandType.COMMAND_TYPE_PRESS).setText("left").build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitBareCopy(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (isRevisionBox(context)) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_HIDE_REVISION_BOX)
          .setText("copy")
          .build()
      );
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_PRESS)
        .addAllModifiers(commandOrControl(context))
        .setText("c")
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitBareCut(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_PRESS)
        .addAllModifiers(commandOrControl(context))
        .setText("x")
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitBareInspect(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    return wrap(
      context,
      Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_SHOW_HOVER).build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitCancel(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_CANCEL).build());
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitClose(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (isRevisionBox(context)) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_HIDE_REVISION_BOX)
          .setText("close")
          .build()
      );
    }

    // if formatted text is given, that means we're closing an application
    Optional<ParseTree> formattedTextNode = node.getChild("formattedText");
    if (formattedTextNode.isPresent()) {
      return commandWithFormattedText(
        context,
        CommandType.COMMAND_TYPE_QUIT,
        node,
        FormattedTextOptions
          .newBuilder()
          .setExpression(false)
          .setConversionMap(Optional.of(textConversionMap))
          .build(),
        true
      );
    }

    if (node.getTerminal("window").isPresent() || node.getTerminal("pane").isPresent()) {
      return wrap(
        context,
        Command.newBuilder().setType(CommandType.COMMAND_TYPE_CLOSE_WINDOW).build()
      );
    }

    // tab commands aren't supported by terminal plugins right now
    if (context.state.getPluginInstalled() && !isTerminal(context)) {
      return wrap(
        context,
        Command.newBuilder().setType(CommandType.COMMAND_TYPE_CLOSE_TAB).build()
      );
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_PRESS)
        .addAllModifiers(commandOrControl(context))
        .setText("w")
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitChange(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canGetState(context)) {
      return requiresSource(context);
    }

    String convertedText = formattedTextConverter.convert(
      treeConverter.convertToEnglish(node.getChild("formattedText")),
      FormattedTextOptions.newBuilder().build(),
      context.state.getLanguage()
    );

    String convertedSelection = formattedTextConverter.convert(
      treeConverter.convertToEnglish(node.getChild("selectionWithImplicitPhrase").get()),
      FormattedTextOptions.newBuilder().build(),
      context.state.getLanguage()
    );

    if (
      formattedTextConverter.isEnclosurePair(convertedText) &&
      formattedTextConverter.isEnclosurePair(convertedSelection)
    ) {
      List<String> toReplace = Arrays.asList(
        convertedSelection.substring(0, convertedSelection.length() / 2),
        convertedSelection.substring(convertedSelection.length() / 2, convertedSelection.length())
      );

      List<String> replacing = Arrays.asList(
        convertedText.substring(0, convertedText.length() / 2),
        convertedText.substring(convertedText.length() / 2, convertedText.length())
      );

      return wrap(
        context,
        commandsFactory
          .create(context.state.getLanguage())
          .changeEnclosures(
            context.state.getSource(),
            context.state.getCursor(),
            toReplace,
            replacing
          )
      );
    }

    ParseTree textNode = node.getChild("formattedText").get();
    Selection selection = treeConverter.convertSelectionWithImplicitPhrase(
      node.getChild("selectionWithImplicitPhrase").get()
    );
    return commandsFactory
      .create(context.state.getLanguage())
      .change(
        context.state.getSource(),
        context.state.getCursor(),
        selection,
        treeConverter.convertToEnglish(textNode),
        context.queue
      )
      .thenApply(
        diffs ->
          diffs
            .stream()
            .map(
              diff ->
                new CommandsResponseAlternativeWithMetadata(
                  context.state,
                  context.parsed,
                  diff,
                  descriptionForReplacement(
                    context.state,
                    node,
                    selection,
                    node.getChild("selectionWithImplicitPhrase").get(),
                    diff
                  )
                )
            )
            .collect(Collectors.toList())
      );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitChangeAll(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return requiresSource(context);
    }

    ParseTree beforeNode = node.getChildren("formattedText").stream().findFirst().get();
    ParseTree afterNode = node.getChildren("formattedText").stream().skip(1).findFirst().get();
    return commandsFactory
      .create(context.state.getLanguage())
      .changeAll(
        context.state.getSource(),
        context.state.getCursor(),
        treeConverter.convertToEnglish(beforeNode),
        treeConverter.convertToEnglish(afterNode),
        context.queue
      )
      .thenApply(
        diffs ->
          diffs
            .stream()
            .map(
              diff -> {
                DescriptionVisitor descriptionVisitor = createDescriptionVisitor(context.state);
                descriptionVisitor.setCodeNode(afterNode, diff.codeForDescription.orElse(""));
                descriptionVisitor.setCodeNode(
                  beforeNode,
                  diff.codeToBeReplacedForDescription.get()
                );

                return new CommandsResponseAlternativeWithMetadata(
                  context.state,
                  context.parsed,
                  diff,
                  descriptionVisitor.visit(node, null)
                );
              }
            )
            .collect(Collectors.toList())
      );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitClick(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    String button = "left";
    if (node.getTerminal("middle").isPresent()) {
      button = "middle";
    } else if (node.getTerminal("right").isPresent()) {
      button = "right";
    }

    String path = "";
    if (node.getChild("formattedText").isPresent()) {
      path = convertFormattedTextNode(node, context);
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_CLICK)
        .setText(button)
        .setPath(path)
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitCommand(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (node.getChild("quantifier").isPresent()) {
      return repeat(
          c -> visit(node.getChildren().get(0), c),
          context,
          treeConverter.convertQuantifier(node.getChild("quantifier").get())
        )
        .thenApply(a -> Arrays.asList(a));
    }

    return visit(node.getChildren().get(0), context);
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitComment(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return requiresSource(context);
    }

    Selection selection = node.getChild("selection").isPresent()
      ? treeConverter.convertSelection(node.getChild("selection").get())
      : new Selection.Builder(ObjectType.LINE).build();

    Diff diff = node.getTerminal("comment").isPresent()
      ? commandsFactory
        .create(context.state.getLanguage())
        .comment(context.state.getSource(), context.state.getCursor(), selection)
      : commandsFactory
        .create(context.state.getLanguage())
        .uncomment(context.state.getSource(), context.state.getCursor(), selection);

    return wrap(context, diff);
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitCopy(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(commandOrControl(context))
          .setText("c")
          .build()
      );
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_COPY)
        .setText(
          commandsFactory
            .create(context.state.getLanguage())
            .copy(
              context.state.getSource(),
              context.state.getCursor(),
              treeConverter.convertSelectionWithImplicitPhrase(
                node.getChild("selectionWithImplicitPhrase").get()
              )
            )
        )
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitCut(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(commandOrControl(context))
          .setText("c")
          .build()
      );
    }

    Selection selection = treeConverter.convertSelectionWithImplicitPhrase(
      node.getChild("selectionWithImplicitPhrase").get()
    );
    Commands commands = commandsFactory.create(context.state.getLanguage());
    return wrap(
      context,
      Arrays.asList(
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_COPY)
          .setText(commands.copy(context.state.getSource(), context.state.getCursor(), selection))
          .build(),
        commands.delete(context.state.getSource(), context.state.getCursor(), selection).toCommand()
      )
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitDebug(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    if (node.getChild("debugContinue").isPresent()) {
      return wrap(
        context,
        Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_CONTINUE).build()
      );
    } else if (node.getChild("debugBreakpoint").isPresent()) {
      if (node.getChild("debugBreakpoint").get().getTerminal("inline").isPresent()) {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_INLINE_BREAKPOINT).build()
        );
      } else {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_TOGGLE_BREAKPOINT).build()
        );
      }
    } else if (node.getChild("debugPause").isPresent()) {
      return wrap(
        context,
        Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_PAUSE).build()
      );
    } else if (node.getChild("debugStep").isPresent()) {
      if (node.getChild("debugStep").get().getTerminal("into").isPresent()) {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_STEP_INTO).build()
        );
      } else if (node.getChild("debugStep").get().getTerminal("out").isPresent()) {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_STEP_OUT).build()
        );
      } else {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_STEP_OVER).build()
        );
      }
    } else if (node.getChild("debugStartStop").isPresent()) {
      if (node.getChild("debugStartStop").get().getTerminal("start").isPresent()) {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_START).build()
        );
      } else {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_STOP).build()
        );
      }
    }

    return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_INVALID).build());
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitDelete(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canGetState(context)) {
      return requiresSource(context);
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .delete(
          context.state.getSource(),
          context.state.getCursor(),
          treeConverter.convertSelectionWithImplicitPhrase(
            node.getChild("selectionWithImplicitPhrase").get()
          )
        )
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitDictate(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return visitInsert(node, context);
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitDictateStart(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(
      context,
      Command.newBuilder().setType(CommandType.COMMAND_TYPE_START_DICTATE).build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitDictateStop(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(
      context,
      Command.newBuilder().setType(CommandType.COMMAND_TYPE_STOP_DICTATE).build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitDuplicate(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return requiresSource(context);
    }

    boolean duplicateAbove = false;
    if (node.getTerminal("above").isPresent()) {
      duplicateAbove = true;
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .duplicate(
          context.state.getSource(),
          context.state.getCursor(),
          treeConverter.convertSelection(node.getChild("selection").get()),
          duplicateAbove
        )
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitEdit(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    String action = "edit";
    if (!context.state.getClientIdentifier().contains("1.8.")) {
      action = "all";
      if (node.getTerminal("clipboard").isPresent()) {
        action = "clipboard";
      } else if (
        node.getTerminal("this").isPresent() ||
        node.getTerminal("that").isPresent() ||
        node.getTerminal("selection").isPresent()
      ) {
        action = "selection";
      }
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_SHOW_REVISION_BOX)
        .setText(action)
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitFocus(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return commandWithFormattedText(
      context,
      CommandType.COMMAND_TYPE_FOCUS,
      node,
      FormattedTextOptions
        .newBuilder()
        .setExpression(false)
        .setConversionMap(Optional.of(textConversionMap))
        .build(),
      true
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitGoTo(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canGetState(context)) {
      return requiresSource(context);
    }

    Selection selection;
    if (node.getChild("positionSelection").isPresent()) {
      selection = treeConverter.convertPositionSelection(node.getChild("positionSelection").get());
    } else {
      selection =
        treeConverter.convertNavigationPositionSelection(
          node.getChild("navigationPositionSelection").get()
        );
    }

    if (node.getChild("preposition").isPresent()) {
      if (selection.count.isPresent()) {
        Selection.Builder builder = new Selection.Builder(selection);
        if (selection.direction == SearchDirection.NEXT) {
          builder.setEndpoint(SelectionEndpoint.END);
        }
        selection = builder.build();
      }

      return wrap(
        context,
        commandsFactory
          .create(context.state.getLanguage())
          .goToUsingPreposition(
            context.state.getSource(),
            context.state.getCursor(),
            selection,
            treeConverter.convertPreposition(node.getChild("preposition"))
          )
      );
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .goTo(context.state.getSource(), context.state.getCursor(), selection)
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitGoToDefinition(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    return wrap(
      context,
      Command.newBuilder().setType(CommandType.COMMAND_TYPE_GO_TO_DEFINITION).build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitGoToPhrase(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    ParseTree selectionNode = node
      .getChildren("positionPhraseRequiredSelection")
      .stream()
      .findFirst()
      .orElseGet(() -> node.getChildren("positionPhraseSelection").stream().findFirst().get());

    // if we're in a browser but aren't in an editor, then alias "go to" to "open"
    if (isBrowser(context) && context.state.getFilename().equals("")) {
      return navigateTo(context, node, selectionNode);
    }

    if (!canGetState(context)) {
      return requiresSource(context);
    }

    Selection selection = treeConverter.convertPhraseSelection(selectionNode);
    if (node.getChild("preposition").isPresent()) {
      return wrap(
        context,
        commandsFactory
          .create(context.state.getLanguage())
          .goToUsingPreposition(
            context.state.getSource(),
            context.state.getCursor(),
            selection,
            treeConverter.convertPreposition(node.getChild("preposition"))
          )
      );
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .goTo(context.state.getSource(), context.state.getCursor(), selection)
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitGoToSyntaxError(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return requiresSource(context);
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .goToSyntaxError(context.state.getSource(), context.state.getCursor())
        .orElseThrow(() -> new ObjectNotFound())
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitForward(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (isBrowser(context)) {
      if (context.state.getPluginInstalled()) {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_FORWARD).build()
        );
      }

      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(Arrays.asList(isMac(context) ? "command" : "alt"))
          .setText("right")
          .build()
      );
    }

    if (context.state.getPluginInstalled()) {
      return wrap(
        context,
        commandsFactory
          .create(context.state.getLanguage())
          .goInDirection(
            context.state.getSource(),
            context.state.getCursor(),
            ArrowKeyDirection.RIGHT
          )
      );
    }

    return wrap(
      context,
      Command.newBuilder().setType(CommandType.COMMAND_TYPE_PRESS).setText("right").build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitIndent(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return requiresSource(context);
    }

    Selection selection = node.getChild("selection").isPresent()
      ? treeConverter.convertSelection(node.getChild("selection").get())
      : new Selection.Builder(ObjectType.LINE).build();

    int level = node.getChild("quantifier").isPresent()
      ? treeConverter.convertQuantifier(node.getChild("quantifier").get())
      : 1;
    Diff diff = node.getTerminal("indent").isPresent()
      ? commandsFactory
        .create(context.state.getLanguage())
        .indent(context.state.getSource(), context.state.getCursor(), selection, level)
      : commandsFactory
        .create(context.state.getLanguage())
        .dedent(context.state.getSource(), context.state.getCursor(), selection, level);

    return wrap(context, diff);
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitInsert(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    InsertDirection direction = InsertDirection.NONE;
    if (node.getTerminal("above").isPresent()) {
      direction = InsertDirection.ABOVE;
    } else if (node.getTerminal("below").isPresent()) {
      direction = InsertDirection.BELOW;
    }

    return insert(context, direction, true, node.getChild("formattedText").get(), node);
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitLaunch(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return commandWithFormattedText(
      context,
      CommandType.COMMAND_TYPE_LAUNCH,
      node,
      FormattedTextOptions
        .newBuilder()
        .setExpression(false)
        .setConversionMap(Optional.of(textConversionMap))
        .build(),
      true
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitLanguageMode(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    String languageName = treeConverter.convertToEnglish(node.getChild("validLanguage").get());
    Language languageMode = languageName.equals("auto")
      ? Language.LANGUAGE_NONE
      : languageDeterminer.fromApiName(languageName);

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_LANGUAGE_MODE)
        .setLanguage(languageMode)
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitPrepositionSelection(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canGetState(context)) {
      return requiresSource(context);
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .goToUsingPreposition(
          context.state.getSource(),
          context.state.getCursor(),
          treeConverter.convertPositionSelectionWithImplicitPhrase(
            node.getChild("positionSelectionWithImplicitPhrase").get()
          ),
          treeConverter.convertPreposition(node.getChild("preposition"))
        )
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitInspect(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    return wrap(
      context,
      Arrays.asList(
        commandsFactory
          .create(context.state.getLanguage())
          .goTo(
            context.state.getSource(),
            context.state.getCursor(),
            treeConverter.convertSelectionWithImplicitPhrase(
              node.getChild("selectionWithImplicitPhrase").get()
            )
          )
          .toCommand(),
        Command.newBuilder().setType(CommandType.COMMAND_TYPE_DEBUGGER_SHOW_HOVER).build()
      )
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitJoinLines(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return requiresSource(context);
    }

    int number = 1;
    if (node.getChild("number").isPresent()) {
      number = treeConverter.convertNumber(node.getChild("number").get());
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .joinLines(context.state.getSource(), context.state.getCursor(), number)
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitNewline(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return wrap(
        context,
        Command.newBuilder().setType(CommandType.COMMAND_TYPE_PRESS).setText("enter").build()
      );
    }

    InsertDirection direction = InsertDirection.NONE;
    if (node.getTerminal("above").isPresent()) {
      direction = InsertDirection.ABOVE;
    } else if (node.getTerminal("below").isPresent()) {
      direction = InsertDirection.BELOW;
    }

    if (node.getChild("addPrefix").isPresent() && direction == InsertDirection.NONE) {
      direction = InsertDirection.BELOW;
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .newline(context.state.getSource(), context.state.getCursor(), direction)
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitNext(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    // in NUX mode, "next" controls nux
    if (context.state.getNux()) {
      return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_NEXT).build());
    }

    return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_INVALID).build());
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitOpenFile(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (isBrowser(context)) {
      return navigateTo(context, node, node.getChild("formattedText").get());
    }

    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_OPEN_FILE_LIST)
        .setPath(convertFormattedTextNode(node, context))
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitPaste(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    Command.Builder command = Command.newBuilder().setType(CommandType.COMMAND_TYPE_CLIPBOARD);
    if (node.getTerminal("below").isPresent()) {
      command.setDirection("below");
    } else if (node.getTerminal("above").isPresent()) {
      command.setDirection("above");
    } else if (node.getTerminal("here").isPresent() || node.getTerminal("inline").isPresent()) {
      command.setDirection("inline");
    }

    if (!canSetState(context)) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(commandOrControl(context))
          .setText("v")
          .build()
      );
    }

    return wrap(context, command.build());
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitPause(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_PAUSE).build());
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitPress(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    List<String> keys = new ArrayList<>();
    List<String> modifiers = new ArrayList<>();

    if (node.getChild("implicitKey").isPresent()) {
      keys =
        Arrays.asList(keyConverter.convert(node.getChild("implicitKey").get(), isMac(context)));
    } else {
      for (int i = 0; i < node.getChildren().size(); i++) {
        ParseTree childNode = node.getChildren().get(i);
        if (childNode.getType().equals("modifierKey")) {
          modifiers.add(keyConverter.convert(childNode));
        } else if (childNode.getType().equals("key")) {
          keys.add(keyConverter.convert(childNode, isMac(context)));
        }
      }

      // if we have only modifiers, then use the last one as a key
      if (modifiers.size() > 0 && keys.size() == 0) {
        keys.add(modifiers.remove(modifiers.size() - 1));
      }
    }

    // chaining works faster if we can use a diff rather than a system press, so special-case saying
    // "left" or "right" in an editor
    if (
      context.state.getPluginInstalled() &&
      node.getChild("implicitKey").isPresent() &&
      keys.size() == 1
    ) {
      if (keys.get(0).equals("left")) {
        return wrap(
          context,
          commandsFactory
            .create(context.state.getLanguage())
            .goInDirection(
              context.state.getSource(),
              context.state.getCursor(),
              ArrowKeyDirection.LEFT
            )
        );
      } else if (keys.get(0).equals("right")) {
        return wrap(
          context,
          commandsFactory
            .create(context.state.getLanguage())
            .goInDirection(
              context.state.getSource(),
              context.state.getCursor(),
              ArrowKeyDirection.RIGHT
            )
        );
      }
    }

    return wrap(
      context,
      keys
        .stream()
        .map(
          key ->
            Command
              .newBuilder()
              .setType(CommandType.COMMAND_TYPE_PRESS)
              .addAllModifiers(modifiers)
              .setText(key)
              .build()
        )
        .collect(Collectors.toList())
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitRedo(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_REDO).build());
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitReload(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (isBrowser(context) && context.state.getPluginInstalled()) {
      return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_RELOAD).build());
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_PRESS)
        .addAllModifiers(commandOrControl(context))
        .setText("r")
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitRename(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    Selection selection = treeConverter.convertSelection(node.getChild("selection").get());
    return commandsFactory
      .create(context.state.getLanguage())
      .rename(
        context.state.getSource(),
        context.state.getCursor(),
        selection,
        treeConverter.convertToEnglish(node.getChild("formattedText").get()),
        context.queue
      )
      .thenApply(
        diffs ->
          diffs
            .stream()
            .map(
              diff -> {
                return new CommandsResponseAlternativeWithMetadata(
                  context.state,
                  context.parsed,
                  diff,
                  descriptionForReplacement(
                    context.state,
                    node,
                    selection,
                    node.getChild("selection").get(),
                    diff
                  )
                );
              }
            )
            .collect(Collectors.toList())
      );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitRepeat(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    int count = node.getChild("quantifier").isPresent()
      ? treeConverter.convertQuantifier(node.getChild("quantifier").get())
      : 1;
    return repeat(c -> repeatCommand(node, c), context, count)
      .thenApply(
        alternative -> {
          alternative = new CommandsResponseAlternativeWithMetadata(alternative);
          alternative.description =
            Optional.of(
              (
                count == 1
                  ? "repeat: "
                  : "repeat " +
                  treeConverter.convertQuantifier(node.getChild("quantifier").get()) +
                  " times: "
              ) +
              alternative.description.orElse("")
            );
          return Arrays.asList(alternative);
        }
      );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitRun(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return insert(context, InsertDirection.NONE, true, node.getChild("formattedText").get(), node)
      .thenApply(
        alternatives ->
          alternatives
            .stream()
            .map(
              alternative -> {
                alternative.commands =
                  alternative.commands
                    .stream()
                    .map(
                      command ->
                        command.getType() == CommandType.COMMAND_TYPE_DIFF
                          ? Command
                            .newBuilder(command)
                            .setType(CommandType.COMMAND_TYPE_RUN)
                            .build()
                          : command
                    )
                    .collect(Collectors.toList());
                return alternative;
              }
            )
            .collect(Collectors.toList())
      );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitSave(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(
      context,
      context.state.getPluginInstalled() && !isBrowser(context)
        ? Command.newBuilder().setType(CommandType.COMMAND_TYPE_SAVE).build()
        : Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(commandOrControl(context))
          .setText("s")
          .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitScroll(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    String key = node.getChild("arrowKeyDirection").isPresent()
      ? treeConverter.convertToDirectionString(node).orElse("down")
      : "down";

    if (context.state.getPluginInstalled() && (isBrowser(context) || isApp(context, "hyper"))) {
      return wrap(
        context,
        Command.newBuilder().setType(CommandType.COMMAND_TYPE_SCROLL).setDirection(key).build()
      );
    }

    if (key.equals("up")) {
      key = "pageup";
    } else if (key.equals("down")) {
      key = "pagedown";
    }

    if (isApp(context, "iterm") && context.state.getPluginInstalled()) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(Arrays.asList("control"))
          .setText(key)
          .build()
      );
    }

    return wrap(
      context,
      Command.newBuilder().setType(CommandType.COMMAND_TYPE_PRESS).setText(key).build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitScrollPhrase(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (isBrowser(context) && context.state.getPluginInstalled()) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_SCROLL)
          .setPath(convertFormattedTextNode(node, context))
          .build()
      );
    }

    return requiresPlugin(context);
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitSelect(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled() && context.parsed.transcript().equals("select all")) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(commandOrControl(context))
          .setText("a")
          .build()
      );
    }

    Selection selection = treeConverter.convertSelectionWithImplicitPhrase(
      node.getChild("selectionWithImplicitPhrase").get()
    );
    Range range = commandsFactory
      .create(context.state.getLanguage())
      .select(context.state.getSource(), context.state.getCursor(), selection);

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_SELECT)
        .setSource(context.state.getSource())
        .setCursor(range.start)
        .setCursorEnd(range.stop)
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitSend(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (isRevisionBox(context)) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_HIDE_REVISION_BOX)
          .setText("send")
          .build()
      );
    }

    return wrap(
      context,
      Command.newBuilder().setType(CommandType.COMMAND_TYPE_PRESS).setText("enter").build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitShift(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return requiresSource(context);
    }

    int quantifier = 1;
    Optional<ParseTree> objectNode = Optional.empty();
    if (node.getChild("quantifier").isPresent()) {
      quantifier = treeConverter.convertQuantifier(node.getChild("quantifier").get());
    } else if (node.getChild("selectionObjectSingular").isPresent()) {
      objectNode = Optional.of(node.getChild("selectionObjectSingular").get().getChildren().get(0));
    } else if (node.getChild("selectionObjectPlural").isPresent()) {
      objectNode = Optional.of(node.getChild("selectionObjectPlural").get().getChildren().get(0));
      quantifier = treeConverter.convertCount(node.getChild("count").get());
    }

    Optional<ObjectType> object = Optional.empty();
    if (objectNode.isPresent()) {
      object =
        Optional.of(
          treeConverter.objectTypeConverter.objectNameToObjectType(objectNode.get().getType())
        );
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .shift(
          context.state.getSource(),
          context.state.getCursor(),
          treeConverter.convertSelection(node.getChild("selection").get()),
          node.getTerminal("right").isPresent() || node.getTerminal("down").isPresent(),
          quantifier,
          object
        )
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitShow(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!isBrowser(context)) {
      return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_INVALID).build());
    }

    String text = "links";
    if (node.getTerminal("inputs").isPresent()) {
      text = "inputs";
    }
    if (node.getTerminal("code").isPresent()) {
      text = "code";
    }

    String path = "";
    if (node.getChild("formattedText").isPresent()) {
      path = convertFormattedTextNode(node, context);
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_SHOW)
        .setText(text)
        .setPath(path)
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitSort(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }
    if (node.getChild("selection").isPresent()) {
      return wrap(
        context,
        commandsFactory
          .create(context.state.getLanguage())
          .sort(
            context.state.getSource(),
            context.state.getCursor(),
            treeConverter.convertSelection(node.getChild("selection").get())
          )
      );
    }

    ObjectType objectType = ObjectType.IMPORT;
    if (node.getTerminal("methods").isPresent()) {
      objectType = ObjectType.METHOD;
    } else if (node.getTerminal("functions").isPresent()) {
      objectType = ObjectType.FUNCTION;
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .sortNodes(context.state.getSource(), context.state.getCursor(), objectType)
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitShowDictationBox(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(
      context,
      Command.newBuilder().setType(CommandType.COMMAND_TYPE_SHOW_REVISION_BOX).build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitSplit(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_SPLIT)
        .setDirection(treeConverter.convertToDirectionString(node).orElse("down"))
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitStyle(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    StylerType stylerType = context.state.getStylers().get(context.state.getLanguage());
    if (stylerType == StylerType.STYLER_TYPE_EDITOR) {
      return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_STYLE).build());
    }

    return wrap(
      context,
      styler.style(
        Diff.fromInitialState(context.state.getSource(), context.state.getCursor()),
        context.state.getLanguage(),
        stylerType
      )
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitSetTextStyle(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canGetState(context)) {
      return requiresSource(context);
    }

    return wrap(
      context,
      commandsFactory
        .create(context.state.getLanguage())
        .styleSelection(
          context.state.getSource(),
          context.state.getCursor(),
          treeConverter.convertSelectionWithImplicitPhrase(
            node.getChild("selectionWithImplicitPhrase").get()
          ),
          treeConverter.convertTextStyle(node.getChild("textStyle").get())
        )
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitSurroundWith(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!canSetState(context)) {
      return requiresSource(context);
    }

    ParseTree textNode = node.getChild("formattedText").get();
    Selection selection = treeConverter.convertSelectionWithImplicitPhrase(
      node.getChild("selectionWithImplicitPhrase").get()
    );
    return commandsFactory
      .create(context.state.getLanguage())
      .surroundWith(
        context.state.getSource(),
        context.state.getCursor(),
        selection,
        treeConverter.convertToEnglish(textNode),
        context.queue
      )
      .thenApply(
        diffs ->
          diffs
            .stream()
            .map(
              diff ->
                new CommandsResponseAlternativeWithMetadata(
                  context.state,
                  context.parsed,
                  diff,
                  descriptionForReplacement(
                    context.state,
                    node,
                    selection,
                    node.getChild("selectionWithImplicitPhrase").get(),
                    diff
                  )
                )
            )
            .collect(Collectors.toList())
      );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitSwitchWindow(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (isMac(context)) {
      return wrap(
        context,
        Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(commandOrControl(context))
          .setText("`")
          .build()
      );
    }
    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_PRESS)
        .addModifiers("alt")
        .setText("tab")
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitSystemInsert(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    boolean dictate = node.getTerminal("dictate").isPresent();
    FormattedTextOptions options = FormattedTextOptions
      .newBuilder()
      .setConversionMap(dictate ? Optional.of(textConversionMap) : Optional.empty())
      .build();

    return commandWithFormattedText(context, CommandType.COMMAND_TYPE_INSERT, node, options, false);
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitTabs(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    // tab commands aren't supported by terminal plugins right now
    boolean supported =
      context.state.getPluginInstalled() && !isApp(context, "iterm") && !isApp(context, "hyper");
    if (node.getTerminal("new").isPresent() || node.getTerminal("create").isPresent()) {
      return wrap(
        context,
        supported
          ? Command.newBuilder().setType(CommandType.COMMAND_TYPE_CREATE_TAB).build()
          : Command
            .newBuilder()
            .setType(CommandType.COMMAND_TYPE_PRESS)
            .addAllModifiers(commandOrControl(context))
            .setText("t")
            .build()
      );
    } else if (node.getTerminal("duplicate").isPresent()) {
      return wrap(
        context,
        isBrowser(context) && context.state.getPluginInstalled()
          ? Command.newBuilder().setType(CommandType.COMMAND_TYPE_DUPLICATE_TAB).build()
          : Command.newBuilder().setType(CommandType.COMMAND_TYPE_INVALID).build()
      );
    } else if (node.getTerminal("next").isPresent() || node.getTerminal("right").isPresent()) {
      if (supported) {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_NEXT_TAB).build()
        );
      } else if (isMac(context)) {
        return wrap(
          context,
          Command
            .newBuilder()
            .setType(CommandType.COMMAND_TYPE_PRESS)
            .addAllModifiers(Arrays.asList("command", "shift"))
            .setText("]")
            .build()
        );
      } else {
        return wrap(
          context,
          Command
            .newBuilder()
            .setType(CommandType.COMMAND_TYPE_PRESS)
            .addAllModifiers(Arrays.asList("control"))
            .setText("tab")
            .build()
        );
      }
    } else if (node.getTerminal("previous").isPresent() || node.getTerminal("left").isPresent()) {
      if (supported) {
        return wrap(
          context,
          Command.newBuilder().setType(CommandType.COMMAND_TYPE_PREVIOUS_TAB).build()
        );
      } else if (isMac(context)) {
        return wrap(
          context,
          Command
            .newBuilder()
            .setType(CommandType.COMMAND_TYPE_PRESS)
            .addAllModifiers(Arrays.asList("command", "shift"))
            .setText("[")
            .build()
        );
      } else {
        return wrap(
          context,
          Command
            .newBuilder()
            .setType(CommandType.COMMAND_TYPE_PRESS)
            .addAllModifiers(Arrays.asList("control", "shift"))
            .setText("tab")
            .build()
        );
      }
    }

    // go to command, since saying go to is optional.
    int index = 1;
    Command.Builder command = Command.newBuilder().setType(CommandType.COMMAND_TYPE_SWITCH_TAB);
    if (node.getChild("number").isPresent()) {
      index = treeConverter.convertNumber(node.getChild("number").get());
    } else if (node.getChild("positional").isPresent()) {
      index = treeConverter.convertPositional(node.getChild("positional").get()) + 1;
    } else {
      // If no number or any other options are specified, default to next tab.
      // This behavior is similar in-line with search results and regular selectors.
      return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_NEXT_TAB).build());
    }

    // most apps alias cmd-9 to be the last tab, rather than the 9th tab
    if (!supported && index < 0) {
      index = 9;
    }

    return wrap(
      context,
      supported
        ? Command.newBuilder().setType(CommandType.COMMAND_TYPE_SWITCH_TAB).setIndex(index).build()
        : Command
          .newBuilder()
          .setType(CommandType.COMMAND_TYPE_PRESS)
          .addAllModifiers(commandOrControl(context))
          .setText(Integer.toString(index))
          .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitType(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    InsertDirection direction = InsertDirection.NONE;
    if (node.getTerminal("above").isPresent()) {
      direction = InsertDirection.ABOVE;
    } else if (node.getTerminal("below").isPresent()) {
      direction = InsertDirection.BELOW;
    }

    return insert(context, direction, false, node.getChild("formattedText").get(), node);
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitUndo(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_UNDO).build());
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitUndoCloseTab(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!isBrowser(context)) {
      return wrap(context, Command.newBuilder().setType(CommandType.COMMAND_TYPE_INVALID).build());
    }

    List<String> modifiers = new ArrayList<>();
    modifiers.addAll(commandOrControl(context));
    modifiers.add("shift");
    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_PRESS)
        .addAllModifiers(modifiers)
        .setText("t")
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitUse(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_USE)
        .setIndex(treeConverter.convertNumber(node.getChild("number").get()))
        .build()
    );
  }

  public CompletableFuture<List<CommandsResponseAlternativeWithMetadata>> visitWindow(
    ParseTree node,
    CommandsVisitorContext context
  ) {
    if (!context.state.getPluginInstalled()) {
      return requiresPlugin(context);
    }

    return wrap(
      context,
      Command
        .newBuilder()
        .setType(CommandType.COMMAND_TYPE_WINDOW)
        .setDirection(treeConverter.convertToDirectionString(node).orElse("right"))
        .build()
    );
  }
}
