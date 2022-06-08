package core.selector;

import core.ast.Ast;
import core.ast.api.AstNode;
import core.ast.api.DefaultAstParent;
import core.closeness.ClosestObjectFinder;
import core.exception.ObjectNotFound;
import core.formattedtext.DefaultConversionMap;
import core.formattedtext.FormattedTextConverter;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import core.language.LanguageSpecific;
import core.util.NumberConverter;
import core.util.ObjectType;
import core.util.Range;
import core.util.RangeSorter;
import core.util.SearchDirection;
import core.util.TextStyle;
import core.util.TextStyler;
import core.util.Whitespace;
import core.util.selection.Selection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

public abstract class SelectorMap implements LanguageSpecific {

  public abstract Language language();

  protected abstract Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors();

  @Inject
  ClosestObjectFinder closestObjectFinder;

  @Inject
  DefaultConversionMap conversionMap;

  @Inject
  FormattedTextConverter formattedTextConverter;

  @Inject
  NumberConverter numberConverter;

  @Inject
  RangeSorter rangeSorter;

  @Inject
  TextStyler textStyler;

  @Inject
  Whitespace whitespace;

  public Function<RawSelectionContext, Stream<Range>> lineRanges = regexMatch(".*");
  public Map<ObjectType, Function<RawSelectionContext, Stream<Range>>> rawSelectors = new HashMap<>();
  public Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> astSelectors = new HashMap<>();

  protected void initialize() {
    initializeRawSelectors();
    initializeAstSelectors();
  }

  private String allStylesPattern(String word) {
    return (
      "(" +
      Stream
        .of(TextStyle.values())
        .map(style -> textStyler.style(word, style))
        .distinct()
        .collect(Collectors.joining("|")) +
      ")"
    );
  }

  private Range applyDirection(
    int closestObjectIndex,
    int objectCount,
    int indexOffset,
    SearchDirection direction,
    SearchDirection closestObjectDirection
  ) {
    // If we already moved an object in the intended direction due to the closest
    // object, we reduce the requested offset by one.
    int alreadyAtIndexOffset = closestObjectDirection != SearchDirection.NONE &&
      direction == closestObjectDirection
      ? -1
      : 0;
    int offset = alreadyAtIndexOffset + indexOffset;
    Range range = new Range(0);
    if (direction == SearchDirection.PREVIOUS) {
      range.stop = closestObjectIndex - offset;
      range.start = range.stop - objectCount;
    } else if (direction == SearchDirection.NONE) {
      range.start = closestObjectIndex + offset;
      range.stop = range.start + objectCount;
    } else if (direction == SearchDirection.NEXT) {
      range.start = closestObjectIndex + 1 + offset;
      range.stop = range.start + objectCount;
    }

    return range;
  }

  private Stream<Range> characterRanges(RawSelectionContext ctx) {
    int start = whitespace.lineNonWhitespaceStart(ctx.source, ctx.cursor);
    int size = whitespace.lineEnd(ctx.source, ctx.cursor) - start;
    Range range = ctx.selection.absoluteRange
      .map(r -> resolveNegativeIndices(r, size))
      .map(absoluteRange -> new Range(start + absoluteRange.start, start + absoluteRange.stop))
      .orElseGet(() -> relativeRange(ctx.cursor, ctx.selection, SearchDirection.NONE));

    if (range.start < 0 || range.start > ctx.source.length()) {
      throw new ObjectNotFound();
    }

    // return a stream of just one element because we've already fully resolved the selection
    return Stream.<Range>of(range);
  }

  private String phraseSearchPattern(String text) {
    if (!text.matches("[0-9A-Za-z ']+")) {
      throw new RuntimeException("Non-alphanumeric symbols, may cause infinite loop if we proceed");
    }

    List<String> result = new ArrayList<>();
    List<String> split = Arrays.asList(numberConverter.convertNumbers(text).split(" "));

    // in order for change to support symbols, we want to take the longest possible symbol match
    int i = 0;
    while (i < split.size()) {
      int symbolEnd = -1;
      for (int j = i; j <= split.size(); j++) {
        if (conversionMap.symbolMap.containsKey(split.subList(i, j))) {
          symbolEnd = j;
        }
      }

      if (symbolEnd > -1) {
        List<String> symbol = split.subList(i, symbolEnd);
        result.add(
          "(" +
          allStylesPattern(String.join("\\s*", symbol)) +
          "|" +
          Pattern.quote(conversionMap.symbolMap.get(split.subList(i, symbolEnd))) +
          ")"
        );
        i = symbolEnd;
      } else {
        int index = i;
        result.add("(" + allStylesPattern(split.get(index)) + ")");
        i++;
      }
    }

    // also just format it with the default formatting text scheme and try that.
    return (
      "(" +
      String.join("[\\-_\\s]*", result) +
      ")|(" +
      Pattern.quote(
        formattedTextConverter.convert(
          text,
          FormattedTextOptions.newBuilder().setExpression(false).build(),
          language()
        )
      ) +
      ")"
    );
  }

  private int minimumSharedWithReferencePoint(Selection selection) {
    // minimum context size to handle both negative and positive indices.
    return selection.absoluteRange
      .map(range -> Math.max(-range.start, range.stop))
      .orElse(Integer.MAX_VALUE);
  }

  private <T, S extends SelectionContext<T>> Map<T, List<T>> referencePointToLocations(
    S ctx,
    BiFunction<S, T, T> locationToReferencePoint,
    Stream<T> locationsStream,
    int minSize
  ) {
    List<T> locationsList = locationsStream.collect(Collectors.toList());
    Map<T, List<T>> referencePointToLocations = locationsList
      .stream()
      // note that java maintains the order of the lists.
      .collect(Collectors.groupingBy(l -> locationToReferencePoint.apply(ctx, l)));
    referencePointToLocations.entrySet().removeIf(e -> e.getValue().size() < minSize);
    return referencePointToLocations;
  }

  private Range resolveNegativeIndices(Range absoluteRange, int length) {
    return new Range(
      absoluteRange.start < 0 ? length + absoluteRange.start : absoluteRange.start,
      absoluteRange.stop <= 0 ? length + absoluteRange.stop : absoluteRange.stop
    );
  }

  private Range relativeRange(int index, Selection selection, SearchDirection currentDirection) {
    int count = selection.count.orElse(1);
    int offset = selection.offset.orElse(0);
    return applyDirection(index, count, offset, selection.direction, currentDirection);
  }

  private Function<AstSelectionContext, Stream<AstNode>> statementSelector() {
    return ctx ->
      instanceMatch(Ast.Statement.class).apply(ctx).filter(e -> !(e instanceof Ast.EnclosedBody));
  }

  protected <T, S extends SelectionContext<T>> Map<ObjectType, Function<S, Stream<T>>> applyIndexing(
    Map<ObjectType, Function<S, Stream<T>>> selectors
  ) {
    Map<ObjectType, Function<S, Stream<T>>> result = new HashMap<>();
    result.putAll(
      selectors
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> applyIndexing(e.getValue())))
    );

    return result;
  }

  protected <T, S extends SelectionContext<T>> Function<S, Stream<T>> applyIndexing(
    Function<S, Stream<T>> locations
  ) {
    return ctx -> {
      Stream<T> locationStream = locations.apply(ctx);
      List<T> locationList = locationStream.collect(Collectors.toList());
      Range positionRange = ctx.selection.absoluteRange
        .map(range -> resolveNegativeIndices(range, locationList.size()))
        .orElseGet(
          () -> {
            T bestMatch;
            if (ctx.selection.object != ObjectType.PHRASE && ctx.selection.name.isPresent()) {
              List<T> exactMatches = locationList
                .stream()
                .filter(
                  e -> textStyler.toLowerCase(ctx.nameString(e)).equals(ctx.selection.name.get())
                )
                .collect(Collectors.toList());
              if (exactMatches.size() > 0) {
                bestMatch = ctx.closestLocation(closestObjectFinder, exactMatches);
              } else {
                bestMatch = ctx.closestLocation(closestObjectFinder, locationList);
              }
            } else {
              bestMatch = ctx.closestLocation(closestObjectFinder, locationList);
            }
            int index = locationList.indexOf(bestMatch);
            return relativeRange(index, ctx.selection, ctx.closestLocationDirection(bestMatch));
          }
        );

      int maxPosition = locationList.size();
      int backward = Math.max(0, Math.min(maxPosition, positionRange.start));
      int forward = Math.max(0, Math.min(maxPosition, positionRange.stop));
      return locationList.subList(backward, forward).stream();
    };
  }

  protected Stream<Range> blockRanges(RawSelectionContext ctx) {
    List<Range> nonWhiteSpaceLineRanges = lineRanges
      .apply(ctx)
      .filter(range -> !whitespace.isWhitespace(ctx.source, range))
      .collect(Collectors.toList());

    List<Range> blockRanges = new ArrayList<Range>();

    // Merge adjacent ranges
    Range blockRange = new Range(nonWhiteSpaceLineRanges.get(0));
    for (int i = 1; i < nonWhiteSpaceLineRanges.size(); i++) {
      if (nonWhiteSpaceLineRanges.get(i).start - 1 != blockRange.stop) {
        blockRanges.add(blockRange);
        blockRange = new Range(nonWhiteSpaceLineRanges.get(i));
      } else {
        blockRange.stop = nonWhiteSpaceLineRanges.get(i).stop;
      }
    }

    blockRanges.add(blockRange);
    return blockRanges.stream();
  }

  protected Stream<AstNode> comments(AstSelectionContext ctx) {
    return ctx.root.tree().comments.stream().map(e -> e);
  }

  protected Stream<AstNode> commentTexts(AstSelectionContext ctx) {
    return ctx.root.tree().comments.stream().map(c -> c.text());
  }

  protected <T, S extends SelectionContext<T>> Function<S, Stream<T>> concat(
    Function<S, Stream<T>> a,
    Function<S, Stream<T>> b
  ) {
    return ctx -> {
      Stream<T> firstNodes;
      try {
        firstNodes = a.apply(ctx);
      } catch (ObjectNotFound e) {
        return b.apply(ctx);
      }
      Stream<T> secondNodes;
      try {
        secondNodes = b.apply(ctx);
      } catch (ObjectNotFound e) {
        return a.apply(ctx);
      }
      return rangeSorter
        .preorder(Stream.concat(firstNodes, secondNodes).collect(Collectors.toList()), ctx::range)
        .stream();
    };
  }

  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> defaultAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();

    m.put(ObjectType.ARGUMENT, filterByParent(instanceMatch(Ast.Argument.class)));
    m.put(ObjectType.ARGUMENT_LIST, instanceMatch(Ast.ArgumentList.class));
    m.put(ObjectType.ASSERT, instanceMatch(Ast.Assert.class));
    m.put(ObjectType.ASSIGNMENT, assignmentSelector());
    m.put(ObjectType.ASSIGNMENT_VAL, instanceMatch(Ast.AssignmentValue.class));
    m.put(ObjectType.ASSIGNMENT_VARIABLE, instanceMatch(Ast.AssignmentVariable.class));
    m.put(ObjectType.BEGIN, instanceMatch(Ast.BeginClause.class));
    m.put(ObjectType.BODY, instanceMatch(Ast.StatementList.class));
    m.put(ObjectType.BREAK, instanceMatch(Ast.Break.class));
    m.put(ObjectType.CALL, filterByParent(instanceMatch(Ast.Call.class)));
    m.put(ObjectType.CASE, filterByParent(instanceMatch(Ast.Case.class)));
    m.put(ObjectType.CATCH, instanceMatch(Ast.CatchClause.class));
    m.put(ObjectType.CLASS, instanceMatch(Ast.Class_.class));
    m.put(ObjectType.COMMENT, this::comments);
    m.put(ObjectType.COMMENT_TEXT, this::commentTexts);
    m.put(ObjectType.CONTINUE, instanceMatch(Ast.Continue.class));
    m.put(ObjectType.CONDITION, instanceMatch(Ast.Condition.class));
    m.put(ObjectType.CONSTRUCTOR, instanceMatch(Ast.Constructor.class));
    m.put(ObjectType.DECORATOR, instanceMatch(Ast.Decorator.class));
    m.put(ObjectType.DECLARATION, declarationSelector());
    m.put(ObjectType.DEFAULT, instanceMatch(Ast.SwitchDefault.class));
    m.put(ObjectType.DEFER, instanceMatch(Ast.Defer.class));
    m.put(ObjectType.DICTIONARY, instanceMatch(Ast.Dictionary.class));
    m.put(ObjectType.DO_WHILE, instanceMatch(Ast.DoWhile.class));
    m.put(
      ObjectType.ELEMENT,
      predicateMatch(node -> node instanceof Ast.ListElement || node instanceof Ast.MarkupElement)
    );
    m.put(ObjectType.ELSE, instanceMatch(Ast.ElseClause.class));
    m.put(ObjectType.ELSE_IF, instanceMatch(Ast.ElseIfClause.class));
    m.put(ObjectType.ENSURE, instanceMatch(Ast.EnsureClause.class));
    m.put(ObjectType.ENUM, instanceMatch(Ast.Enum.class));
    m.put(ObjectType.EXCEPT, instanceMatch(Ast.CatchClause.class));
    m.put(ObjectType.FINALLY, instanceMatch(Ast.FinallyClause.class));
    m.put(
      ObjectType.FOR,
      predicateMatch(node -> node instanceof Ast.ForClause || node instanceof Ast.ForEachClause)
    );
    m.put(ObjectType.FOREACH, predicateMatch(node -> node instanceof Ast.ForEachClause));
    m.put(ObjectType.FUNCTION, instanceMatch(Ast.Function.class));
    m.put(ObjectType.IF, instanceMatch(Ast.IfClause.class));
    m.put(ObjectType.IMPLEMENTATION, instanceMatch(Ast.Implementation.class));
    m.put(ObjectType.IMPORT, instanceMatch(Ast.Import.class));
    m.put(
      ObjectType.INTERFACE,
      concat(
        applyIndexing(filterByParent(instanceMatch(Ast.ImplementsType.class))),
        applyIndexing(instanceMatch(Ast.Interface.class))
      )
    );
    m.put(ObjectType.INTERFACE_LIST, instanceMatch(Ast.ImplementsList.class));
    m.put(ObjectType.KEY, instanceMatch(Ast.KeyValuePairKey.class));
    m.put(ObjectType.KEY_VALUE_PAIR, instanceMatch(Ast.KeyValuePair.class));
    m.put(ObjectType.LAMBDA, instanceMatch(Ast.Lambda.class));
    m.put(ObjectType.LIST, instanceMatch(Ast.List_.class));
    m.put(ObjectType.LOOP, instanceMatch(Ast.Loop.class));
    m.put(ObjectType.METHOD, instanceMatch(Ast.Method.class));
    m.put(ObjectType.MODIFIER_LIST, instanceMatch(Ast.ModifierList.class));
    m.put(ObjectType.MODIFIER, instanceMatch(Ast.Modifier.class));
    m.put(ObjectType.NAMESPACE, filterByParent(instanceMatch(Ast.Namespace.class)));
    m.put(ObjectType.PARAMETER, filterByParent(instanceMatch(Ast.Parameter.class)));
    m.put(ObjectType.PARAMETER_LIST, instanceMatch(Ast.ParameterList.class));
    m.put(ObjectType.PARENT, filterByParent(instanceMatch(Ast.ExtendsType.class)));
    m.put(ObjectType.PARENT_LIST, instanceMatch(Ast.ExtendsList.class));
    m.put(ObjectType.PROTOTYPE, instanceMatch(Ast.Prototype.class));
    m.put(ObjectType.PROPERTY, propertySelector());
    m.put(ObjectType.RAISE, instanceMatch(Ast.Throw.class));
    m.put(ObjectType.RECEIVER_ARGUMENT, instanceMatch(Ast.ReceiverArgument.class));
    m.put(ObjectType.RESCUE, instanceMatch(Ast.RescueClause.class));
    m.put(ObjectType.RETURN, instanceMatch(Ast.Return.class));
    m.put(ObjectType.RETURN_TYPE, returnTypeSelector());
    m.put(ObjectType.RETURN_VAL, instanceMatch(Ast.ReturnValue.class));
    m.put(ObjectType.RETURN_VALUE_NAME, instanceMatch(Ast.ReturnValueName.class));
    m.put(ObjectType.RETURN_VALUE_NAME_LIST, instanceMatch(Ast.ReturnValueNameList.class));
    m.put(ObjectType.SET, instanceMatch(Ast.Set_.class));
    m.put(ObjectType.STATEMENT, statementSelector());
    m.put(ObjectType.STRING, instanceMatch(Ast.String_.class));
    m.put(ObjectType.STRING_TEXT, instanceMatch(Ast.StringText.class));
    m.put(ObjectType.STRUCT, instanceMatch(Ast.Struct.class));
    m.put(ObjectType.SWITCH, instanceMatch(Ast.Switch.class));
    m.put(ObjectType.SYNCHRONIZED, instanceMatch(Ast.Synchronized.class));
    m.put(ObjectType.THROW, instanceMatch(Ast.Throw.class));
    m.put(ObjectType.TRAIT, instanceMatch(Ast.Trait.class));
    m.put(ObjectType.TRY, instanceMatch(Ast.TryClause.class));
    m.put(ObjectType.TUPLE, instanceMatch(Ast.Tuple.class));
    m.put(ObjectType.TYPE, instanceMatch(Ast.Type.class));
    m.put(ObjectType.TYPE_ALIAS, instanceMatch(Ast.TypeAlias.class));
    m.put(ObjectType.TYPE_ARGUMENT, filterByParent(instanceMatch(Ast.TypeArgument.class)));
    m.put(ObjectType.TYPE_ARGUMENT_LIST, instanceMatch(Ast.TypeArgumentList.class));
    m.put(ObjectType.TYPE_PARAMETER, filterByParent(instanceMatch(Ast.TypeParameter.class)));
    m.put(ObjectType.TYPE_PARAMETER_LIST, instanceMatch(Ast.TypeParameterList.class));
    m.put(ObjectType.UNTIL, instanceMatch(Ast.Until.class));
    m.put(ObjectType.USING, instanceMatch(Ast.Using.class));
    m.put(ObjectType.WHILE, instanceMatch(Ast.While.class));
    m.put(ObjectType.WITH, instanceMatch(Ast.With.class));
    m.put(ObjectType.WITH_ITEM, instanceMatch(Ast.WithItem.class));
    m.put(ObjectType.WITH_ALIAS, instanceMatch(Ast.WithItemAlias.class));
    m.put(
      ObjectType.VALUE,
      predicateMatch(
        node ->
          node instanceof Ast.AssignmentValue ||
          node instanceof Ast.KeyValuePairValue ||
          node instanceof Ast.MarkupAttributeValue ||
          node instanceof Ast.ReturnValue
      )
    );

    m = filterByName(m);
    m = applyIndexing(m);
    return m;
  }

  protected void initializeAstSelectors() {
    astSelectors.putAll(defaultAstSelectors());
    astSelectors.putAll(languageAstSelectors());
  }

  protected void initializeRawSelectors() {
    rawSelectors.put(ObjectType.ALL, ctx -> Stream.<Range>of(new Range(0, ctx.source.length())));
    rawSelectors.put(ObjectType.BLOCK, this::blockRanges);
    rawSelectors.put(ObjectType.FILE, ctx -> Stream.<Range>of(new Range(0, ctx.source.length())));
    rawSelectors.put(ObjectType.LINE, this.lineRanges);
    rawSelectors.put(
      ObjectType.NUMBER,
      filterByLine(
        regexMatch("([0-9]+\\.[0-9]+)[^\\.[0-9]]|([0-9]+)", this::numberStart, this::numberEnd)
      )
    );
    rawSelectors.put(ObjectType.PHRASE, this::phraseRanges);
    rawSelectors.put(ObjectType.VERTICAL, this::verticalRanges);
    rawSelectors.put(ObjectType.TERM, filterByLine(regexMatch("[^\\s]+")));
    rawSelectors.put(
      ObjectType.SYMBOL,
      filterByName(filterByLine(regexMatch("[^\\sa-zA-Z0-9_]+")))
    );
    rawSelectors.put(
      ObjectType.WORD,
      filterByName(filterByLine(regexMatch("[a-zA-Z0-9_]+|[^\\sa-zA-Z0-9_]+")))
    );
    rawSelectors = applyIndexing(rawSelectors);

    // we intentionally don't use applyIndexing to character ranges to avoid creating
    // a range for every character in the file
    rawSelectors.put(ObjectType.CHARACTER, this::characterRanges);
    rawSelectors.put(ObjectType.LETTER, this::characterRanges);
  }

  protected Function<RawSelectionContext, Stream<Range>> filterByLine(
    Function<RawSelectionContext, Stream<Range>> ranges
  ) {
    return filterBySharedReferencePoint(
      (ctx, range) -> {
        int position = range.start;
        int start = whitespace.lineStart(ctx.source, position);
        int stop = whitespace.lineEnd(ctx.source, position);
        return new Range(start, stop);
      },
      ranges
    );
  }

  protected <T, S extends SelectionContext<T>> Function<S, Stream<T>> filterByName(
    Function<S, Stream<T>> locations
  ) {
    return ctx -> {
      Stream<T> locationStream = locations.apply(ctx);
      if (ctx.selection.name.isPresent()) {
        Pattern pattern = Pattern.compile(phraseSearchPattern(ctx.selection.name.get()));
        locationStream = locationStream.filter(e -> pattern.matcher(ctx.nameString(e)).find());
      }

      return locationStream;
    };
  }

  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> filterByName(
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> selectors
  ) {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> result = new HashMap<>();
    result.putAll(
      selectors
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> filterByName(e.getValue())))
    );

    return result;
  }

  protected Function<AstSelectionContext, Stream<AstNode>> filterByParent(
    Function<AstSelectionContext, Stream<AstNode>> nodes
  ) {
    return filterBySharedReferencePoint((ctx, node) -> node.parent().get(), nodes);
  }

  /**
   * Set reference node that we count from. For nodes that are part of a list, we don't want to
   * start indexing at the start of the file. Instead, provide a reference point from which we'll
   * start indexing, which is usually some parent of the node we're looking for.
   *
   * @param reference Mapping from a node to its reference node
   * @param locations Selection function for nodes (i.e., not reference nodes)
   */
  protected <T, S extends SelectionContext<T>> Function<S, Stream<T>> filterBySharedReferencePoint(
    BiFunction<S, T, T> referencePoint,
    Function<S, Stream<T>> locations
  ) {
    return ctx -> {
      Stream<T> locationStream = locations.apply(ctx);
      int minimumSharedWithReferencePoint = minimumSharedWithReferencePoint(ctx.selection);
      if (minimumSharedWithReferencePoint == Integer.MAX_VALUE) {
        return locationStream;
      }

      // look for the closest reference point that is large enough to support the
      // absolute indexes used.
      Map<T, List<T>> referencePointToLocations = referencePointToLocations(
        ctx,
        referencePoint,
        locationStream,
        minimumSharedWithReferencePoint
      );
      if (referencePointToLocations.size() == 0) {
        return Stream.empty();
      }

      // Get the closest reference point in specified direction.
      // Note that the grammar doesn't support NEXT / PREVIOUS in this case, so this could be
      // simplified if we decide we don't want this functionality.
      List<T> referencePoints = rangeSorter.preorder(
        referencePointToLocations.keySet(),
        ctx::range
      );

      T closestLocation = ctx.closestLocation(closestObjectFinder, referencePoints);
      int index = referencePoints.indexOf(closestLocation);
      index =
        applyDirection(
          index,
          1,
          0,
          ctx.selection.direction,
          ctx.closestLocationDirection(closestLocation)
        )
          .start;
      if (index < 0 || index >= referencePoints.size()) {
        return Stream.empty();
      }

      T closestReferencePoint = referencePoints.get(index);
      return referencePointToLocations.get(closestReferencePoint).stream();
    };
  }

  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> htmlAstSelectors() {
    // Html selectors without aliases you can make in an html-only context, like
    // mapping "value" to "assignment value"
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    m.put(ObjectType.ATTRIBUTE, instanceMatch(Ast.MarkupAttribute.class));
    m.put(ObjectType.ATTRIBUTE_NAME, instanceMatch(Ast.MarkupAttributeName.class));
    m.put(ObjectType.ATTRIBUTE_VAL, instanceMatch(Ast.MarkupAttributeValue.class));
    m.put(ObjectType.COMMENT, this::comments);
    m.put(ObjectType.COMMENT_TEXT, this::commentTexts);
    m.put(ObjectType.CONTENT, instanceMatch(Ast.MarkupContentList.class));
    m.put(ObjectType.TAG, instanceMatch(Ast.MarkupElement.class));
    m.put(ObjectType.OPEN_TAG, instanceMatch(Ast.MarkupOpeningTag.class));
    m.put(ObjectType.CLOSE_TAG, instanceMatch(Ast.MarkupClosingTag.class));
    m = filterByName(m);
    m = applyIndexing(m);
    return m;
  }

  protected Function<AstSelectionContext, Stream<AstNode>> instanceMatch(
    Class<? extends AstNode> type
  ) {
    return ctx -> ctx.root.find(type).map(e -> e);
  }

  protected int numberEnd(Matcher m) {
    int end = m.end(1);
    if (end == -1) {
      return m.end(2);
    }

    return end;
  }

  protected int numberStart(Matcher m) {
    int start = m.start(1);
    if (start == -1) {
      return m.start(2);
    }

    return start;
  }

  protected Stream<Range> phraseRanges(RawSelectionContext ctx) {
    // we should never create a phrase selection without the text field.
    return regexMatch(phraseSearchPattern(ctx.selection.name.get())).apply(ctx);
  }

  protected Function<AstSelectionContext, Stream<AstNode>> predicateMatch(
    Predicate<AstNode> predicate
  ) {
    return ctx -> ctx.root.filter(predicate);
  }

  protected Function<AstSelectionContext, Stream<AstNode>> assignmentSelector() {
    return predicateMatch(
      p ->
        (
          (
            p instanceof Ast.VariableDeclaration &&
            ((DefaultAstParent) p).child(Ast.AssignmentList.class).isPresent() &&
            ((DefaultAstParent) p).child(Ast.AssignmentList.class)
              .get()
              .child(Ast.Assignment.class)
              .isPresent()
          ) ||
          p instanceof Ast.Assignment
        )
    );
  }

  protected Function<AstSelectionContext, Stream<AstNode>> declarationSelector() {
    return predicateMatch(
      p ->
        (
          p instanceof Ast.VariableDeclaration &&
          !((DefaultAstParent) p).find(Ast.AssignmentValueList.class).findFirst().isPresent()
        )
    );
  }

  protected Function<AstSelectionContext, Stream<AstNode>> propertySelector() {
    return predicateMatch(
      p ->
        // First condition appears to be for kotlin. Second is for ruby
        (
          p instanceof Ast.Statement &&
          p.parent().filter(parent -> parent instanceof Ast.Member).isPresent()
        ) ||
        (
          p instanceof Ast.Assignment &&
          p.parent().filter(parent -> parent instanceof Ast.Member).isPresent()
        ) ||
        p instanceof Ast.Property ||
        p instanceof Ast.KeyValuePair
    );
  }

  protected Function<AstSelectionContext, Stream<AstNode>> returnTypeSelector() {
    return predicateMatch(
      p ->
        (
          p instanceof Ast.Type &&
          p.parent().isPresent() &&
          p.parent().get() instanceof Ast.TypeOptional &&
          p.parent().get().parent().isPresent() &&
          (
            p.parent().get().parent().get() instanceof Ast.Function ||
            p.parent().get().parent().get() instanceof Ast.Method
          )
        )
    );
  }

  protected Function<AstSelectionContext, Stream<AstNode>> returnTypeSelectorForPythonAndJavascript() {
    return predicateMatch(
      p ->
        (
          p instanceof Ast.Type &&
          p.parent().isPresent() &&
          (
            p.parent().get() instanceof Ast.ReturnTypeOptional ||
            p.parent().get() instanceof Ast.TypeOptional
          )
        )
    );
  }

  protected Function<AstSelectionContext, Stream<AstNode>> goMethodSelector() {
    return predicateMatch(
      p ->
        (
          p instanceof Ast.Method ||
          (
            p instanceof Ast.Function &&
            (((DefaultAstParent) p).find(Ast.ReceiverArgument.class).findFirst().isPresent())
          )
        )
    );
  }

  protected Function<RawSelectionContext, Stream<Range>> regexMatch(String pattern) {
    return regexMatch(pattern, m -> m.start(), m -> m.end());
  }

  protected Function<RawSelectionContext, Stream<Range>> regexMatch(
    String pattern,
    Function<Matcher, Integer> start,
    Function<Matcher, Integer> end
  ) {
    Pattern p = Pattern.compile(pattern);
    return ctx -> {
      List<Range> ranges = new ArrayList<>();
      Matcher m = p.matcher(ctx.source);
      int nextIndex = 0;
      while (m.find(nextIndex)) {
        ranges.add(new Range(start.apply(m), end.apply(m)));
        nextIndex = end.apply(m);
        if (nextIndex >= ctx.source.length()) {
          break;
        }
        if (nextIndex < ctx.source.length() && ctx.source.charAt(nextIndex) == '\n') {
          nextIndex++;
        }
      }

      return ranges.stream();
    };
  }

  protected Stream<Range> verticalRanges(RawSelectionContext ctx) {
    int vertical = ctx.cursor - whitespace.lineStart(ctx.source, ctx.cursor);
    return lineRanges
      .apply(ctx)
      .map(
        r -> {
          int position = Math.min(r.start + vertical, r.stop);
          return new Range(position, position);
        }
      );
  }
}
