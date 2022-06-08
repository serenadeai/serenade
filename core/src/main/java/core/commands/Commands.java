package core.commands;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.api.AstList;
import core.ast.api.AstListWithLinePadding;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstSyntaxError;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.ast.api.IndentationUtils;
import core.closeness.ClosestObjectFinder;
import core.codeengine.CodeEngineBatchQueue;
import core.codeengine.Resolver;
import core.exception.CannotGoInDirection;
import core.exception.DiffHasNoEffect;
import core.exception.LanguageFeatureNotSupported;
import core.exception.NextObjectNotFound;
import core.exception.ObjectNotFound;
import core.exception.ObjectTooFarAway;
import core.formattedtext.ConversionMap;
import core.formattedtext.ConversionMapFactory;
import core.formattedtext.FormattedTextConverter;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import core.language.LanguageSpecific;
import core.metadata.DiffWithMetadata;
import core.selector.Selector;
import core.snippet.Snippet;
import core.snippet.SnippetCollection;
import core.snippet.SnippetCollectionFactory;
import core.util.ArrowKeyDirection;
import core.util.Diff;
import core.util.InsertDirection;
import core.util.LinePositionConverter;
import core.util.ObjectType;
import core.util.ObjectTypeConverter;
import core.util.Preposition;
import core.util.Range;
import core.util.RangeSorter;
import core.util.SearchDirection;
import core.util.TextStyle;
import core.util.TextStyler;
import core.util.Whitespace;
import core.util.selection.Selection;
import core.util.selection.SelectionEndpoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import toolbelt.languages.LanguageDeterminer;

public class Commands implements LanguageSpecific {

  @Inject
  AstFactory astFactory;

  @Inject
  ClosestObjectFinder closestObjectFinder;

  @Inject
  ConversionMapFactory conversionMapFactory;

  @Inject
  FormattedTextConverter formattedTextConverter;

  @Inject
  IndentationUtils indentationUtils;

  @Inject
  LanguageDeterminer languageDeterminer;

  @Inject
  ObjectTypeConverter objectTypeConverter;

  @Inject
  Resolver resolver;

  @Inject
  RangeSorter rangeSorter;

  @Inject
  Selector.Factory selectorFactory;

  @Inject
  SnippetCollectionFactory snippetCollectionFactory;

  @Inject
  TextStyler textStyler;

  @Inject
  Whitespace whitespace;

  private Pattern tagPattern = Pattern.compile("<.+?>.*</.+?>");

  @FunctionalInterface
  interface FunctionOrMethodBuilder {
    public DefaultAstParent apply(String name, List<String> parameters, String bodyString);
  }

  @Inject
  public Commands() {}

  public Language language() {
    return Language.LANGUAGE_DEFAULT;
  }

  private void deleteNode(AstNode node) {
    // When we say "delete body", we really mean "delete contents of statement list".
    // This is mainly to let pass replacement happen in the statement list node, but
    // we could technically move some of the logic to suite and remove this.
    if (node instanceof AstList) {
      ((AstList) node).clear();
    } else {
      node.remove();
    }
  }

  private DiffWithMetadata deleteNodes(String source, int cursor, Selection selection) {
    Selector selector = selectorFactory.create(language());
    AstParent root = astFactory.createFileRoot(source, language());
    List<AstNode> nodes = selector.nodesFromSelection(source, cursor, selection, root);
    if (nodes.size() == 0) {
      throw new ObjectNotFound();
    }

    // remove all but the last node,which will be handled on its own.
    for (int i = 0; i < nodes.size() - 1; i++) {
      deleteNode(nodes.get(i));
    }

    AstNode lastNode = nodes.get(nodes.size() - 1);
    List<AstToken> tokensBeforeLastNode = new ArrayList<>(
      root.tokens().subList(0, lastNode.tokenRange().get().start)
    );

    deleteNode(lastNode);

    // closest token on the left that still remains after the remove.
    int i = 0;
    for (
      ;
      i < tokensBeforeLastNode.size() &&
      i < root.tokens().size() &&
      tokensBeforeLastNode.get(i).code.equals(root.tokens().get(i).code);
      i++
    ) {}

    i--;
    int newCursor = i > 0 ? root.tokens().get(i).range().stop : 0;
    return new DiffWithMetadata(
      Diff
        .fromInitialState(source, cursor)
        .replaceSource(root.codeWithCommentsAndWhitespace())
        .moveCursor(newCursor)
    );
  }

  private List<Range> getEnclosureRanges(String source, List<String> enclosure) {
    List<Range> ranges = new ArrayList<>();
    Stack<Integer> foundEnclosureStarts = new Stack<>();

    boolean inString = false;
    boolean inChar = false;
    for (int i = 0; i < source.length();) {
      if (
        (enclosure.get(0).equals("\"") || enclosure.get(0).equals("\'")) &&
        i > 0 &&
        source.charAt(i - 1) == '\\'
      ) {
        i++;
        continue;
      } else if (!enclosure.get(0).equals("\"") && source.charAt(i) == '\"') {
        inString = !inString;
      } else if (!enclosure.get(0).equals("\'") && source.charAt(i) == '\'') {
        inChar = !inChar;
      }

      if (
        source.substring(i, i + enclosure.get(1).length()).equals(enclosure.get(1)) &&
        !foundEnclosureStarts.empty() &&
        !inString &&
        !inChar
      ) {
        ranges.add(new Range(foundEnclosureStarts.pop(), i + enclosure.get(1).length()));
        i += enclosure.get(1).length();
      } else if (
        source.substring(i, i + enclosure.get(0).length()).equals(enclosure.get(0)) &&
        !inString &&
        !inChar
      ) {
        foundEnclosureStarts.push(i);
        i += enclosure.get(0).length();
      } else {
        i++;
      }
    }

    return ranges;
  }

  private Range includeNewlineIfCoincidesWithLineEndpoints(String source, Range range) {
    Range result = new Range(range.start, range.stop);
    if (
      (range.start == 0 || source.charAt(range.start - 1) == '\n') &&
      (range.stop < source.length() && source.charAt(range.stop) == '\n')
    ) {
      // include newline
      result.stop++;
    }

    return result;
  }

  private List<DiffWithMetadata> throwErrorIfChangeHasNoEffect(List<DiffWithMetadata> diffs) {
    List<DiffWithMetadata> result = diffs
      .stream()
      .filter(diff -> !diff.codeToBeReplacedForDescription.equals(diff.codeForDescription))
      .collect(Collectors.toList());
    if (result.size() == 0) {
      throw new DiffHasNoEffect();
    }
    return result;
  }

  protected Diff astCodeWithCursorAtEndOfNode(
    AstNode root,
    String source,
    AstNode node,
    int cursor
  ) {
    Diff result = Diff
      .fromInitialState(source, cursor)
      .replaceSource(root.codeWithCommentsAndWhitespace())
      .moveCursor(node.range().stop);

    return result;
  }

  protected String codeWithIndentationLevel(String source, int level, String body) {
    String indentation = whitespace.indentationForLevel(
      source,
      level,
      conversionMapFactory.create(language()).indentation()
    );
    String indentedBody = "";
    String[] lines = body.split("\n");
    for (String line : lines) {
      if (!line.equals("")) {
        indentedBody += indentation;
      }

      indentedBody += line + "\n";
    }

    return indentedBody;
  }

  protected AstParent scopingBlock(AstNode node) {
    return scopingBlocks(node).findFirst().get();
  }

  protected Stream<AstParent> scopingBlocks(AstNode node) {
    return node
      .ancestors()
      .filter(
        e ->
          e instanceof Ast.Class_ ||
          e instanceof Ast.Function ||
          e instanceof Ast.StatementList ||
          e instanceof Ast.Method
      );
  }

  protected void setStyleOptions(FormattedTextOptions.Builder options, String toReplace) {
    if (toReplace.length() == 0) {
      return;
    }

    boolean underscorePrefix = toReplace.charAt(0) == '_';
    if (underscorePrefix) {
      toReplace = toReplace.substring(1, toReplace.length());
    }

    Set<TextStyle> styles = textStyler.getStyle(toReplace);
    options.setStyle(styles.iterator().next());
    if (
      underscorePrefix &&
      styles.contains(TextStyle.LOWERCASE) &&
      styles.contains(TextStyle.UNDERSCORES)
    ) {
      // prioritize underscores if its a single word prefixed with underscore.
      options.setStyle(TextStyle.UNDERSCORES);
    } else if (styles.contains(TextStyle.ALL_CAPS)) {
      // prioritize pascal case over pascal case.
      options.setStyle(TextStyle.ALL_CAPS);
    } else if (styles.contains(TextStyle.PASCAL_CASE)) {
      // prioritize pascal case over capitalization.
      options.setStyle(TextStyle.PASCAL_CASE);
    } else if (styles.contains(TextStyle.LOWERCASE)) {
      // prioritize lower case over camel case v.
      options.setStyle(TextStyle.LOWERCASE);
    }
  }

  protected Set<String> variablesInScope(AstNode root, AstNode node) {
    List<AstParent> blocks = scopingBlocks(node).collect(Collectors.toList());
    return root
      .filter(
        e ->
          (
            e instanceof Ast.AssignmentVariable ||
            e instanceof Ast.Parameter ||
            e instanceof Ast.BlockIterator
          ) &&
          blocks.contains(scopingBlock(e))
      )
      .map(
        e -> {
          Optional<Ast.Identifier> var = e.find(Ast.Identifier.class).findFirst();
          return var.isPresent() ? var.get().code() : "";
        }
      )
      .collect(Collectors.toSet());
  }

  public CompletableFuture<List<DiffWithMetadata>> add(
    String source,
    int cursor,
    String transcript,
    Snippet snippet,
    CodeEngineBatchQueue queue
  ) {
    AstParent root = astFactory.createFileRoot(source, language());
    return snippet
      .apply(source, cursor, root, transcript, queue)
      .thenApply(
        diffs -> {
          for (DiffWithMetadata diff : diffs) {
            diff.nameForDescription = snippet.trigger.description;
          }

          return diffs;
        }
      );
  }

  public CompletableFuture<List<DiffWithMetadata>> add(
    String source,
    int cursor,
    String transcript,
    CodeEngineBatchQueue queue
  ) {
    return add(source, cursor, transcript, snippet(transcript), queue);
  }

  public CompletableFuture<List<DiffWithMetadata>> change(
    String source,
    int cursor,
    Selection selection,
    String english,
    CodeEngineBatchQueue queue
  ) {
    return change(
      source,
      cursor,
      selectorFactory.create(language()).searchForExpandedRange(source, cursor, selection),
      english,
      queue
    );
  }

  public CompletableFuture<List<DiffWithMetadata>> change(
    String source,
    int cursor,
    Range range,
    String english,
    CodeEngineBatchQueue queue
  ) {
    String toReplace = source.substring(range.start, range.stop);
    if (!formattedTextConverter.containsNonAlphaNumeric(english)) {
      FormattedTextOptions.Builder options = FormattedTextOptions.newBuilder();
      setStyleOptions(options, toReplace);
      Set<TextStyle> styles = textStyler.getStyle(toReplace);
      // Style matching is a better predictor than code engine when the style to match isn't ambiguious.
      // It's ambiguous when the word to replace is lower case and we're replacing it with more than one word.
      // e.g., source = "foo" "change foo to bar baz" shouldn't force "bar baz"
      if (
        !styles.contains(TextStyle.UNKNOWN) &&
        !(styles.contains(TextStyle.LOWERCASE) && english.contains(" "))
      ) {
        DiffWithMetadata result = resolver.resolveEnglishWithFormattedText(
          Diff.fromInitialState(source, cursor),
          range,
          language(),
          options.build(),
          english
        );

        result.codeToBeReplacedForDescription = Optional.of(toReplace);
        return CompletableFuture.completedFuture(
          throwErrorIfChangeHasNoEffect(Arrays.asList(result))
        );
      }
    }

    return resolver
      .resolveEnglish(Diff.fromInitialState(source, cursor), range, language(), english, queue)
      .thenApply(
        diffs -> {
          for (DiffWithMetadata diff : diffs) {
            diff.codeToBeReplacedForDescription = Optional.of(toReplace);
          }
          return throwErrorIfChangeHasNoEffect(diffs);
        }
      );
  }

  public CompletableFuture<List<DiffWithMetadata>> changeAll(
    String source,
    int cursor,
    String beforeText,
    String afterText,
    CodeEngineBatchQueue queue
  ) {
    return change(
      source,
      cursor,
      new Selection.Builder(ObjectType.PHRASE).setName(beforeText).build(),
      afterText,
      queue
    )
      .thenApply(
        diffs ->
          diffs
            .stream()
            .map(
              diffWithMetadata -> {
                Diff diff = diffWithMetadata.diff.withoutChanges();
                String beforeFormatted = diffWithMetadata.codeToBeReplacedForDescription.get();
                String afterFormatted = diffWithMetadata.codeForDescription.get();
                String sourceAfterFirstChange = diff.getSource();
                Pattern pattern = Pattern.compile(Pattern.quote(beforeFormatted));
                Matcher matcher = pattern.matcher(sourceAfterFirstChange);
                int nextIndex = 0;
                while (matcher.find(nextIndex)) {
                  diff =
                    diff.replaceRange(new Range(matcher.start(), matcher.end()), afterFormatted);
                  nextIndex = matcher.end();
                  if (nextIndex >= sourceAfterFirstChange.length()) {
                    break;
                  }
                }
                diffWithMetadata.diff = diffWithMetadata.diff.then(diff);
                return diffWithMetadata;
              }
            )
            .collect(Collectors.toList())
      );
  }

  public Diff changeEnclosures(
    String source,
    int cursor,
    List<String> toReplace,
    List<String> replacing
  ) {
    List<Range> enclosureRanges = getEnclosureRanges(source, toReplace);
    if (enclosureRanges.size() == 0) {
      throw new NextObjectNotFound();
    }

    Range nearestEnclosureRange = closestObjectFinder.closestRange(source, cursor, enclosureRanges);
    String replacement =
      replacing.get(0) +
      source.substring(
        nearestEnclosureRange.start + toReplace.get(0).length(),
        nearestEnclosureRange.stop - toReplace.get(1).length()
      ) +
      replacing.get(1);

    return Diff
      .fromInitialState(source, cursor)
      .replaceRangeAndMoveCursorToStop(nearestEnclosureRange, replacement);
  }

  public Diff comment(String source, int cursor, Selection selection) {
    Selector selector = selectorFactory.create(language());
    ConversionMap conversionMap = conversionMapFactory.create(language());
    Range range = selector.searchForExpandedRange(source, cursor, selection);

    // if given a selection that doesn't start with a line, start the
    // range at the nearest start of line.
    int start = whitespace.lineStart(source, range.start);
    int stop = range.stop;

    String prefix = conversionMap.commentPrefix();
    String result = "";
    int i = start;

    // commentPosition keeps the indentation of the first line so subsequent lines
    // keep the same level of indentation for comments
    int commentPosition = Integer.MAX_VALUE;
    boolean foundFirst = false;
    while (i < stop) {
      // search til we find the first non-whitespace character, then comment
      int j = 0;
      while (!foundFirst && whitespace.isWhitespace(source.charAt(i)) && j < commentPosition) {
        result += source.charAt(i);
        j++;
        i++;
      }
      if (!foundFirst) {
        result += prefix;
        commentPosition = j;
      }
      foundFirst = true;

      // add comment postfix at end of line
      if (source.charAt(i) == '\n') {
        foundFirst = false;
      }
      result += source.charAt(i);
      i++;
    }
    result += conversionMap.commentPostfix();

    return Diff
      .fromInitialState(source, cursor)
      .replaceRange(new Range(start, stop), result)
      .moveCursor(start + result.length());
  }

  public String copy(String source, int cursor, Selection selection) {
    Range range = selectorFactory
      .create(language())
      .searchForExpandedRange(source, cursor, selection);
    if (!(selection.fromCursorToObject || selection.endpoint != SelectionEndpoint.NONE || 
    Arrays.asList(ObjectType.BLOCK, ObjectType.CHARACTER, ObjectType.FILE, ObjectType.LINE, ObjectType.PHRASE, ObjectType.TERM,  ObjectType.WORD).contains(selection.object)
    )) {
      range = whitespace.includeAdjacentIndentation(source, range);
    }

    range = includeNewlineIfCoincidesWithLineEndpoints(source, range);
    return source.substring(range.start, range.stop);
  }

  public DiffWithMetadata delete(String source, int cursor, Selection selection) {
    Selector selector = selectorFactory.create(language());
    if (selection.fromCursorToObject || selection.endpoint != SelectionEndpoint.NONE) {
      Range range = selector.searchForExpandedRange(source, cursor, selection);
      return new DiffWithMetadata(
        Diff.fromInitialState(source, cursor).replaceRangeAndMoveCursorToStop(range, "")
      );
    }

    if (selector.isAstSelection(selection)) {
      return deleteNodes(source, cursor, selection);
    }

    Range range = selector.searchForExpandedRange(source, cursor, selection);
    if (selection.object == ObjectType.WORD || selection.object == ObjectType.TERM) {
      range =
        whitespace.followedByNewline(source, range)
          ? whitespace.expandLeft(source, range)
          : whitespace.expandRight(source, range);
    }

    range = includeNewlineIfCoincidesWithLineEndpoints(source, range);
    return new DiffWithMetadata(
      Diff.fromInitialState(source, cursor).replaceRangeAndMoveCursorToStop(range, "")
    );
  }

  public Diff dedent(String source, int cursor, Selection selection, int level) {
    Selector selector = selectorFactory.create(language());
    ConversionMap conversionMap = conversionMapFactory.create(language());
    Range range = selector.rangeFromSelection(source, cursor, selection);

    // if given a selection that doesn't start with a line, start the range
    // at the nearest start of line
    int start = whitespace.lineStart(source, range.start);
    int stop = range.stop;
    int lineNonWhitespaceStart = whitespace.lineNonWhitespaceStart(source, range.start);

    // determine the number of characters to skip
    String token = whitespace.indentationToken(source, conversionMap.indentation());
    int skip = (token.equals("\t")) ? 1 : token.length();

    // each time we hit a newline, skip that number of whitespace characters start one before the first
    // character of the line, since that's the newline of the previous line
    String result = "";
    for (int i = Math.max(0, start - 1); i < stop; i++) {
      result += source.charAt(i);
      if (source.charAt(i) == '\n' || i == 0) {
        for (int j = 0; j < level; j++) {
          for (int k = 0; k < skip; k++) {
            if (source.charAt(i + 1) == ' ') {
              i++;
            }
          }
        }
      }
    }

    return Diff
      .fromInitialState(source, cursor)
      .replaceRange(new Range(Math.max(0, start - 1), stop), result)
      // move cursor to the new non-whitespace start.
      .moveCursor(lineNonWhitespaceStart - (skip * level));
  }

  public Diff duplicate(String source, int cursor, Selection selection, boolean duplicateAbove) {
    Selector selector = selectorFactory.create(language());
    Range range = selector.searchForExpandedRange(source, cursor, selection);
    range = whitespace.includeAdjacentIndentation(source, range);
    String text = source.substring(range.start, range.stop) + "\n";
    int positionAbove = whitespace.previousNewline(source, range.start);
    if (
      (positionAbove >= 0) &&
      whitespace.isWhitespace(
        source.substring(whitespace.lineStart(source, positionAbove), positionAbove)
      ) &&
      duplicateAbove
    ) {
      positionAbove = whitespace.lineStart(source, positionAbove);
      text = text.substring(0, text.length() - 1);
    } else {
      positionAbove++;
    }

    int insertPosition = duplicateAbove
      ? positionAbove
      : whitespace.lineEnd(source, range.stop) + 1;
    int newCursor = insertPosition + text.length() - 1;
    return Diff.fromInitialState(source, cursor).insert(insertPosition, text).moveCursor(newCursor);
  }

  public Diff indent(String source, int cursor, Selection selection, int level) {
    Selector selector = selectorFactory.create(language());
    ConversionMap conversionMap = conversionMapFactory.create(language());
    Range range = selector.rangeFromSelection(source, cursor, selection);

    // if given a selection that doesn't start with a line, start the
    // range at the nearest start of line.
    String indentation = whitespace.indentationForLevel(source, level, conversionMap.indentation());
    int start = whitespace.lineStart(source, range.start);
    int stop = range.stop;

    // each time we hit a newline, insert another indentation token after
    // it and update the cursor position.
    String result = indentation;
    int newCursor = cursor + indentation.length();
    for (int i = start; i < stop; i++) {
      result += source.charAt(i);
      if (source.charAt(i) == '\n') {
        if (i < cursor) {
          newCursor += indentation.length();
        }

        result += indentation;
      }
    }

    return Diff
      .fromInitialState(source, cursor)
      .replaceRange(new Range(start, stop), result)
      .moveCursor(newCursor);
  }

  public DiffWithMetadata goInDirection(String source, int cursor, ArrowKeyDirection direction) {
    Selection selection = new Selection.Builder(
      (direction == ArrowKeyDirection.UP || direction == ArrowKeyDirection.DOWN)
        ? ObjectType.VERTICAL
        : ObjectType.CHARACTER
    )
      .setDirection(
        (direction == ArrowKeyDirection.UP || direction == ArrowKeyDirection.LEFT)
          ? SearchDirection.PREVIOUS
          : SearchDirection.NEXT
      )
      .build();
    try {
      return goTo(source, cursor, selection);
    } catch (ObjectNotFound e) {
      throw new CannotGoInDirection(direction);
    }
  }

  public DiffWithMetadata goTo(String source, int cursor, Selection selection) {
    Selector selector = selectorFactory.create(language());
    Diff diff = Diff.fromInitialState(source, cursor);
    int position = 0;
    try {
      position = selector.searchForRange(source, cursor, selection).start;
    } catch (ObjectNotFound e) {
      if (selection.object == ObjectType.LINE && selection.direction == SearchDirection.NEXT) {
        diff = diff.insert(source.length(), "\n");
        source = source + "\n";
        position = selector.positionFromSelection(source, cursor, selection);
      } else {
        throw e;
      }
    }

    // Go to the next location if there are no other modifiers and we would have not
    // otherwise moved the cursor. A confusing case without the modifier condition was when I
    // was at the end of a line but not sure if there was trailing whitespace and "end of
    // line" brought me to the end of the next line. Note that this matches the behavior of
    // the result command, where "result" is aliased to "next result".
    // The modifier condition also prevents cases like "next word 1", which we don't fully support yet.
    if (cursor != position || selection.hasNonTextModifier()) {
      return new DiffWithMetadata(diff.moveCursor(position));
    }

    Selection nextSelection = new Selection.Builder(selection)
      .setDirection(SearchDirection.NEXT)
      .build();

    try {
      return new DiffWithMetadata(
        diff.moveCursor(selector.positionFromSelection(source, cursor, nextSelection))
      );
    } catch (ObjectNotFound e) {
      throw new NextObjectNotFound();
    }
  }

  public Optional<Diff> goToSyntaxError(String source, int cursor) {
    Diff diff = Diff.fromInitialState(source, cursor);
    Optional<Integer> position = selectorFactory.create(language()).syntaxErrorPosition(source);
    if (position.isPresent()) {
      return Optional.of(diff.moveCursor(position.get()));
    }

    return Optional.empty();
  }

  public DiffWithMetadata goToUsingPreposition(
    String source,
    int cursor,
    Selection selection,
    Preposition preposition
  ) {
    Selector selector = selectorFactory.create(language());

    Diff diff = Diff.fromInitialState(source, cursor);
    Range range = selector.rangeFromSelection(source, cursor, selection);
    if (preposition == Preposition.AFTER) {
      cursor = range.stop;
    } else if (preposition == Preposition.BEFORE) {
      cursor = range.start;
    }

    return new DiffWithMetadata(diff.moveCursor(cursor));
  }

  public CompletableFuture<List<DiffWithMetadata>> insert(
    String source,
    int cursor,
    String english,
    InsertDirection direction,
    boolean overrideSingleSlotOptions,
    Optional<FormattedTextOptions> options,
    CodeEngineBatchQueue queue
  ) {
    int position;
    if (direction == InsertDirection.BELOW) {
      position = whitespace.lineEnd(source, cursor);
    } else if (direction == InsertDirection.ABOVE) {
      position = Math.max(0, whitespace.lineStart(source, cursor) - 1);
    } else {
      position = cursor;
    }

    String prefix = "";
    String postfix = "";
    if (direction != InsertDirection.NONE) {
      prefix = whitespace.indentationAtCursor(source, cursor);
      if (direction == InsertDirection.ABOVE && position == 0) {
        postfix = "\n";
      } else {
        prefix = "\n" + prefix;
      }
    }

    Diff diff = Diff
      .fromInitialState(source, cursor)
      .insertStringAndMoveCursorToStop(position, prefix)
      .insert(position, postfix);

    return resolver
      .resolve(
        diff.withoutChanges(),
        new Range(diff.getCursor(), diff.getCursor()),
        language(),
        resolver.wrapInSlot("snippet") + resolver.wrapInSlot("cursor"),
        Map.of("snippet", english),
        options.map(o -> Map.of("snippet", Arrays.asList(o))).orElse(Collections.emptyMap()),
        Optional.empty(),
        queue,
        overrideSingleSlotOptions
      )
      .thenApply(
        l ->
          l
            .stream()
            .map(
              d -> {
                d.diff = diff.then(d.diff);
                return d;
              }
            )
            .collect(Collectors.toList())
      );
  }

  public Diff joinLines(String source, int cursor, int number) {
    String result = source;
    int index = cursor;
    for (int i = 0; i < number; i++) {
      while (index < result.length() && result.charAt(index) != '\n') {
        index++;
      }
      if (result.charAt(index) == '\n') {
        result = result.substring(0, index) + result.substring(index + 1, result.length());
      }
    }

    return Diff
      .fromInitialState(source, cursor)
      .replaceRange(new Range(0, source.length()), result);
  }

  public DiffWithMetadata newline(String source, int cursor, InsertDirection direction) {
    // don't do this with metadata since it won't work with quantifiers.
    if (direction == InsertDirection.NONE) {
      String newlineWithIndentation =
        "\n" +
        source.substring(
          whitespace.lineStart(source, cursor),
          Math.min(whitespace.lineNonWhitespaceStart(source, cursor), cursor)
        );
      return new DiffWithMetadata(
        Diff
          .fromInitialState(source, cursor)
          .insertStringAndMoveCursorToStop(cursor, newlineWithIndentation)
      );
    }
    String code = whitespace.indentationAtCursor(source, cursor) + "\n";
    int position = direction == InsertDirection.BELOW
      ? whitespace.lineEnd(source, cursor) + 1
      : whitespace.lineStart(source, cursor);

    // no trailing newline, and we need it, so just add it. It's not entirely clear
    // why the newline normalizer doesn't handle this for us.
    if (source.length() + 1 == position) {
      position--;
      code = "\n" + code;
    }

    return new DiffWithMetadata(
      Diff
        .fromInitialState(source, cursor)
        .insert(position, code)
        .moveCursor(position + code.length() - 1)
    );
  }

  public DiffWithMetadata paste(String source, int cursor, String text, InsertDirection direction) {
    // by default, if you just say "paste", then we should paste on the line below if the clipboard
    // ends with a newline, but paste inline if the clipboard does not end with a newline. this is
    // consistent with what most editors (like vim) do.
    if (
      direction == InsertDirection.INLINE ||
      (direction == InsertDirection.NONE && !text.endsWith("\n"))
    ) {
      return new DiffWithMetadata(
        Diff.fromInitialState(source, cursor).insertStringAndMoveCursorToStop(cursor, text)
      );
    }
    boolean addBelow =
      direction.equals(InsertDirection.NONE) || direction.equals(InsertDirection.BELOW);

    // The logic below ensures a newline is at the end, so remove a newline if there is one.
    if (text.endsWith("\n")) {
      text = text.substring(0, text.length() - 1);
    }

    int position = addBelow
      ? whitespace.lineEnd(source, cursor)
      : Math.max(0, whitespace.lineStart(source, cursor) - 1);
    String prefix = "";
    String postfix = "";
    if (!addBelow && position == 0) {
      postfix = "\n";
    } else {
      prefix = "\n" + prefix;
    }

    return new DiffWithMetadata(
      Diff
        .fromInitialState(source, cursor)
        .insert(position, prefix + text + postfix)
        .moveCursor(position + prefix.length() + text.length())
    );
  }

  public Diff shift(
    String source,
    int cursor,
    Selection selection,
    boolean forwardShift,
    int quantifier,
    Optional<ObjectType> object
  ) {
    Selector selector = selectorFactory.create(language());
    SearchDirection direction = forwardShift ? SearchDirection.NEXT : SearchDirection.PREVIOUS;
    Selection.Builder nextOrPreviousSelectionBuilder;
    if (object.isPresent()) {
      nextOrPreviousSelectionBuilder =
        new Selection.Builder(object.get())
          .setDirection(direction)
          .setName(Optional.<String>empty())
          .setCount(quantifier);
    } else {
      nextOrPreviousSelectionBuilder =
        new Selection.Builder(selection)
          .setDirection(direction)
          .setName(Optional.<String>empty())
          .setCount(quantifier);
    }
    Selection nextOrPreviousSelection = nextOrPreviousSelectionBuilder.build();

    Range selectionRange = selector.rangeFromSelection(source, cursor, selection);

    // Change the cursor position to be more robust to changes to the semantics of next and previous
    int swapCursor = selectionRange.start;
    if (
      ((selection.count.isPresent() && selection.count.get() > 1) || object.isPresent()) &&
      direction == SearchDirection.NEXT
    ) {
      swapCursor = selectionRange.stop;
    }
    Range swapRange = selector.rangeFromSelection(source, swapCursor, nextOrPreviousSelection);

    String selectionCode = source.substring(selectionRange.start, selectionRange.stop);
    String swapCode = source.substring(swapRange.start, swapRange.stop);
    String result = "";
    int newCursor = cursor;
    if (forwardShift) {
      String prefix = source.substring(0, selectionRange.start) + swapCode;
      String midfix = source.substring(selectionRange.stop, swapRange.start) + selectionCode;
      String postfix = source.substring(swapRange.stop, source.length());
      newCursor = prefix.length() + midfix.length();
      result = prefix + midfix + postfix;
    } else {
      String prefix = source.substring(0, swapRange.start) + selectionCode;
      String midfix = source.substring(swapRange.stop, selectionRange.start) + swapCode;
      String postfix = source.substring(selectionRange.stop, source.length());
      newCursor = prefix.length();
      result = prefix + midfix + postfix;
    }
    return Diff.fromInitialState(source, cursor).replaceSource(result).moveCursor(newCursor);
  }

  public CompletableFuture<List<DiffWithMetadata>> rename(
    String source,
    int cursor,
    Selection selection,
    String english,
    CodeEngineBatchQueue queue
  ) {
    List<AstNode> nodes = selectorFactory
      .create(language())
      .nodesFromSelection(source, cursor, selection, astFactory.createFileRoot(source, language()));

    if (nodes.size() == 0) {
      throw new ObjectNotFound();
    }

    Optional<Ast.Identifier> identifier = nodes.get(0).name();
    if (!identifier.isPresent()) {
      throw new ObjectNotFound();
    }

    return change(source, cursor, identifier.get().range(), english, queue);
  }

  public Range select(String source, int cursor, Selection selection) {
    return selectorFactory.create(language()).searchForExpandedRange(source, cursor, selection);
  }

  public Snippet snippet(String transcript) {
    return snippetCollectionFactory
      .create(language())
      .snippets()
      .stream()
      .filter(e -> e.trigger.matches(transcript))
      .findFirst()
      .orElseThrow(() -> new LanguageFeatureNotSupported());
  }

  public Diff sort(String source, int cursor, Selection selection) {
    Range range = selectorFactory.create(language()).rangeFromSelection(source, cursor, selection);
    range =
      new Range(whitespace.lineStart(source, range.start), whitespace.lineEnd(source, range.stop));

    String result = source.substring(range.start, range.stop);
    String[] lines = result.split("\n");

    Arrays.sort(lines);
    result = String.join("\n", lines);

    return Diff.fromInitialState(source, cursor).replaceRange(range, result);
  }

  public Diff sortNodes(String source, int cursor, ObjectType objectType) {
    final Class<? extends AstNode> type;
    if (objectType == ObjectType.METHOD) {
      type = Ast.Method.class;
    } else if (objectType == ObjectType.FUNCTION) {
      type = Ast.Function.class;
    } else {
      type = Ast.Import.class;
    }
    AstParent root = astFactory.createFileRoot(source, language());
    Stream<? extends AstNode> allNodesOfType = root.find(type);

    AstNode closestNode = closestObjectFinder.closestNode(root, source, allNodesOfType, cursor);
    AstListWithLinePadding<?> list = closestNode.ancestor(AstListWithLinePadding.class).get();

    List<AstParent> nodes = list
      .elements()
      .stream()
      .filter(
        node ->
          node.children().stream().findFirst().map(child -> type.isInstance(child)).orElse(false)
      )
      .collect(Collectors.toList());

    List<AstParent> sortedNodes = new ArrayList<>(nodes);
    if (type == Ast.Import.class) {
      Collections.sort(sortedNodes, Comparator.comparing(AstParent::code));
    } else {
      Collections.sort(
        sortedNodes,
        Comparator.comparing(member -> member.children().get(0).nameString())
      );
    }

    // Remove comments touching the nodes on the start.
    Map<AstParent, Optional<Ast.Comment>> comments = new HashMap<>();
    for (AstParent node : nodes) {
      Optional<Ast.Comment> comment = node
        .tree()
        .comments.stream()
        .filter(e -> e.lineRange().get().stop == node.lineRange().get().start)
        .findFirst();
      comments.put(node, comment);
      comment.ifPresent(c -> list.remove(c));
    }

    // Probably slightly faster to remove all and then add back.
    sortedNodes.stream().limit(sortedNodes.size() - 1).forEach(node -> node.remove());

    // Intentionally recalculates line range stop every time, since we don't know what styling is doing.
    // A node guaranteed to exist because we found the closest node.
    sortedNodes
      .stream()
      .limit(sortedNodes.size() - 1)
      .forEach(
        node ->
          list.addAtLine(sortedNodes.get(sortedNodes.size() - 1).lineRange().get().start, node)
      );

    // add comments back to corresponding nodes.
    sortedNodes
      .stream()
      .forEach(
        node -> comments.get(node).ifPresent(c -> list.addAtLine(node.lineRange().get().start, c))
      );

    return Diff
      .fromInitialState(source, cursor)
      .replaceSource(root.codeWithCommentsAndWhitespace());
  }

  public Diff styleSelection(String source, int cursor, Selection selection, TextStyle style) {
    Range range = selectorFactory
      .create(language())
      .searchForExpandedRange(source, cursor, selection);
    String styledText = textStyler.style(source.substring(range.start, range.stop), style);

    return Diff
      .fromInitialState(source, cursor)
      .replaceRangeAndMoveCursorToStop(new Range(range.start, range.stop), styledText);
  }

  public CompletableFuture<List<DiffWithMetadata>> surroundWith(
    String source,
    int cursor,
    Selection selection,
    String english,
    CodeEngineBatchQueue queue
  ) {
    ConversionMap conversionMap = conversionMapFactory.create(language());
    Range range = selectorFactory
      .create(language())
      .searchForExpandedRange(source, cursor, selection);
    String original = source.substring(range.start, range.stop);
    if (english.endsWith(" tag")) {
      english = "tag " + english.substring(0, english.length() - " tag".length());
    }

    return resolver
      .resolveEnglish(Diff.fromInitialState(source, cursor), range, language(), english, queue)
      .thenApply(
        diffs ->
          diffs
            .stream()
            .map(
              diff -> {
                String enclosure = diff.codeForDescription.get();
                diff.codeToBeReplacedForDescription = Optional.of(original);
                diff.codeForDescription = Optional.of(enclosure);

                Set<String> enclosureEnds = new HashSet<>(
                  conversionMap.enclosureNameToStop
                    .entrySet()
                    .stream()
                    .map(e -> e.getValue())
                    .collect(Collectors.toSet())
                );
                enclosureEnds.add("</");

                // Heuristic: pick the first maximal length enclosureEnd in the string.
                int split = -1;
                int length = -1;
                for (int i = enclosure.length() - 1; i >= 0; i--) {
                  for (String enclosureEnd : enclosureEnds) {
                    if (
                      i + enclosureEnd.length() <= enclosure.length() &&
                      enclosure.substring(i, i + enclosureEnd.length()).equals(enclosureEnd) &&
                      enclosureEnd.length() > length
                    ) {
                      length = enclosureEnd.length();
                      split = i;
                    }
                  }
                }

                String start = enclosure;
                String end = enclosure;
                if (split > 0) {
                  start = enclosure.substring(0, split);
                  end = enclosure.substring(split);
                }
                diff.diff =
                  Diff
                    .fromInitialState(source, cursor)
                    .insert(range.start, start)
                    .insert(range.stop, end)
                    .moveCursor(range.stop + start.length());

                return diff;
              }
            )
            .collect(Collectors.toList())
      );
  }

  public Diff uncomment(String source, int cursor, Selection selection) {
    ConversionMap conversionMap = conversionMapFactory.create(language());
    Range range = selectorFactory
      .create(language())
      .searchForExpandedRange(source, cursor, selection);

    // if given a selection that doesn't start with a line, start the
    // range at the nearest start of line.
    int start = whitespace.lineStart(source, range.start);
    int stop = range.stop;

    String prefix = conversionMap.commentPrefix();
    String postfix = conversionMap.commentPostfix();
    String result = "";
    int i = start;
    boolean uncommented = false;
    while (i < stop) {
      // if a line starts with whitespace, add it first before uncommenting
      while (!uncommented && whitespace.isWhitespace(source.charAt(i))) {
        result += source.charAt(i);
        i++;
      }

      // remove all consecutive comment prefixes as start of line (not after)
      if (!uncommented && source.substring(i, i + prefix.length()).equals(prefix)) {
        i += prefix.length();
        uncommented = true;
        continue;
      }

      if (source.charAt(i) == '\n') {
        uncommented = false;
      }

      result += source.charAt(i);
      i++;
    }

    return Diff
      .fromInitialState(source, cursor)
      .replaceRange(new Range(start, stop), result)
      .moveCursor(start + result.length());
  }
}
