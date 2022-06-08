package core.closeness;

import core.ast.api.AstList;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.IndentationUtils;
import core.exception.CannotFindInsertionPoint;
import core.exception.ObjectNotFound;
import core.util.LinePositionConverter;
import core.util.LinePositionConverter;
import core.util.Range;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ClosestObjectFinder {

  @Inject
  IndentationUtils indentationUtils;

  @Inject
  RangeComparators comparators;

  @Inject
  Whitespace whitespace;

  @Inject
  public ClosestObjectFinder() {}

  private List<AstNode> ancestorsExcludingSubTree(AstNode node, AstNode subTree) {
    List<AstNode> ancestors = node.ancestors().collect(Collectors.toList());
    Collections.reverse(ancestors);
    ancestors.add(node);
    if (ancestors.contains(subTree)) {
      return ancestors.subList(0, ancestors.indexOf(subTree) + 1);
    }

    return ancestors;
  }

  private int sharedPrefixLength(List<AstNode> a, List<AstNode> b) {
    int shared = 0;
    while (shared < a.size() && shared < b.size() && a.get(shared) == b.get(shared)) {
      shared++;
    }

    return shared;
  }

  private Comparator<AstNode> closestNodeComparator(AstParent root, String source, int cursor) {
    return sharedAncestorComparator(root, source, cursor)
      .thenComparing(Comparator.comparing(AstNode::range, closestRangeComparator(source, cursor)));
  }

  private Comparator<Range> closestRangeComparator(String source, int cursor) {
    return comparators
      .lineDistanceToEndpoint(source, cursor)
      .thenComparing(comparators.distanceToEndpoint(cursor))
      .thenComparing(comparators.distanceToInside(cursor))
      // in python, pick inner body if you're adding to the end of it
      .thenComparing(comparators.nested())
      .thenComparing(comparators.prioritizeRight(source, cursor));
  }

  private <T extends AstList<?>> List<T> listsToPrioritize(
    String source,
    int cursor,
    List<T> lists
  ) {
    // Restrict search to the most nested non-multiline list we're apart of, and all lists it wraps.
    // This effectively prioritizes non-multiline lists we're inside of over nearby multi-line lists, but
    // doesn't do it if we're deeply nested inside of a non-multiline list.
    List<T> restrictedLists = lists
      .stream()
      .filter(l -> !l.isMultiline())
      .map(l -> l.range())
      .filter(r -> r.contains(cursor))
      .min(comparators.nested())
      .map(
        range -> lists.stream().filter(l -> l.range().inside(range)).collect(Collectors.toList())
      )
      .orElse(lists);

    LinePositionConverter linePositionConverter = new LinePositionConverter(source);
    int linePosition = linePositionConverter.position(cursor);
    boolean cursorLineIsWhitespace = whitespace.isWhitespace(
      source.substring(whitespace.lineStart(source, cursor), whitespace.lineEnd(source, cursor))
    );

    // Try to prioritize non-multiline lists where the cursor is between elements (i.e. we're not in the middle of an element).
    List<T> nonMultilineListsWithCursorBetweenElements = restrictedLists
      .stream()
      .filter(
        l ->
          !l.isMultiline() &&
          inside(l.range(), cursor) &&
          l.elements().stream().noneMatch(e -> e.range().start < cursor && cursor < e.range().stop)
      )
      .collect(Collectors.toList());
    if (nonMultilineListsWithCursorBetweenElements.size() > 0) {
      return nonMultilineListsWithCursorBetweenElements;
    }

    // Other try to prioritize injecting "immediately below" on a multi-line list.
    List<T> multilineLists = multilineImmediatelyBelowCursor(
      linePosition,
      cursorLineIsWhitespace,
      restrictedLists
    );
    // Scan to the right when picking lists like <a></a> and if (..) {} that are on the line we're on.
    multilineLists =
      prioritizeRightWhenListsAreOnLine(
        linePositionConverter,
        linePosition,
        cursor,
        multilineLists
      );

    // Break ties in python based on indentation level.
    if (cursorLineIsWhitespace) {
      multilineLists = prioritizeBasedOnBlankLineIndentation(source, cursor, multilineLists);
    }
    if (multilineLists.size() > 0) {
      return multilineLists;
    }

    // If we can't find that, then it's far away, and default to the regular closeness algorithm that uses tree distance,
    // line numbers, etc.
    return restrictedLists;
  }

  private <T extends AstList<?>> List<T> multilineImmediatelyBelowCursor(
    int linePosition,
    boolean cursorLineIsWhitespace,
    List<T> lists
  ) {
    int lineOffset = cursorLineIsWhitespace ? 0 : 1;
    return lists
      .stream()
      .filter(
        l -> {
          if (!l.isMultiline()) {
            return false;
          }
          List<Integer> availableLines = l.availableLines();
          // can add on the line below.
          if (availableLines.contains(linePosition + lineOffset)) {
            return true;
          }
          // the next line is "{" by itself, and can insert below that one.
          if (availableLines.contains(linePosition + lineOffset + 1)) {
            int potentialBraceIndex = l.tokenRangeWithCommentsAndWhitespace().start - 1;
            if (potentialBraceIndex > 1) {
              AstToken braceToken = l.tree().tokens().get(potentialBraceIndex);
              AstToken tokenBeforeBraceToken = l
                .tree()
                .tokens()
                .get(braceToken.tokenRangeWithCommentsAndWhitespace().start - 1);
              if (
                braceToken.code.equals("{") &&
                braceToken.lineRange().get().start == linePosition + lineOffset &&
                tokenBeforeBraceToken.lineRange().get().stop == linePosition + lineOffset
              ) {
                return true;
              }
            }
          }
          return false;
        }
      )
      .collect(Collectors.toList());
  }

  private <T extends AstList<?>> List<T> prioritizeBasedOnBlankLineIndentation(
    String source,
    int cursor,
    List<T> lists
  ) {
    String cursorToBeginningOfLine = source.substring(whitespace.lineStart(source, cursor), cursor);
    List<T> result = lists
      .stream()
      .filter(n -> indentationUtils.indent(n).equals(cursorToBeginningOfLine))
      .collect(Collectors.toList());
    if (result.size() > 0) {
      return result;
    }
    return lists;
  }

  private <T extends AstList<?>> List<T> prioritizeRightWhenListsAreOnLine(
    LinePositionConverter linePositionConverter,
    int linePosition,
    int cursor,
    List<T> lists
  ) {
    // handle cases adding to lists like <a></a> and if (..) {} by looking to the right of the cursor.
    List<T> listsOnLine = lists
      .stream()
      .filter(
        l ->
          linePositionConverter
            .range(l.rangeWithCommentsAndWhitespace())
            .equals(new Range(linePosition))
      )
      .collect(Collectors.toList());

    // only scan line to right if we aren't inside of something.
    if (listsOnLine.stream().anyMatch(l -> inside(l.rangeWithCommentsAndWhitespace(), cursor))) {
      return lists;
    }

    List<T> listsToRight = listsOnLine
      .stream()
      .filter(l -> l.rangeWithCommentsAndWhitespace().start >= cursor)
      .collect(Collectors.toList());
    if (listsToRight.size() > 0) {
      return listsToRight;
    }

    // if there's nothing left on this line, keep scanning beyond the line.
    List<T> result = new ArrayList<>(lists);
    result.removeAll(listsOnLine);
    if (result.size() > 0) {
      return result;
    }

    return lists;
  }

  private Comparator<AstNode> sharedAncestorComparator(AstParent root, String source, int cursor) {
    List<AstToken> tokensTouching = root
      .tokens()
      .stream()
      .filter(e -> cursor >= e.range().start && cursor <= e.range().stop)
      .collect(Collectors.toList());

    if (tokensTouching.size() == 0) {
      return (a, b) -> 0;
    }

    Optional<AstToken> visibleTokenTouching = tokensTouching
      .stream()
      .filter(e -> e.parent().isPresent())
      .findFirst();

    List<AstNode> tokenAncestors;
    if (visibleTokenTouching.isPresent()) {
      // if we're in a visible token, then use that token's ancestors
      tokenAncestors = visibleTokenTouching.get().ancestors().collect(Collectors.toList());
      Collections.reverse(tokenAncestors);
      tokenAncestors.add(visibleTokenTouching.get());
    } else {
      // if not, take the intersection of the ancestors of the visible tokens to the left and to the
      // right of the cursor
      int tokenIndex = root.tokens().indexOf(tokensTouching.get(0));
      Optional<List<AstNode>> ancestorsOfLeftVisible = root
        .tokens()
        .stream()
        .limit(tokenIndex)
        .filter(e -> e.parent().isPresent())
        .reduce((a, b) -> b)
        .map(e -> e.ancestors().collect(Collectors.toList()));
      ancestorsOfLeftVisible.ifPresent(Collections::reverse);

      Optional<List<AstNode>> ancestorsOfRightVisible = root
        .tokens()
        .stream()
        .skip(tokenIndex)
        .filter(e -> e.parent().isPresent())
        .findFirst()
        .map(e -> e.ancestors().collect(Collectors.toList()));
      ancestorsOfRightVisible.ifPresent(Collections::reverse);

      if (ancestorsOfLeftVisible.isEmpty() && ancestorsOfRightVisible.isEmpty()) {
        tokenAncestors = new ArrayList<>();
      } else if (ancestorsOfRightVisible.isEmpty()) {
        tokenAncestors = ancestorsOfLeftVisible.get();
      } else if (ancestorsOfLeftVisible.isEmpty()) {
        tokenAncestors = ancestorsOfRightVisible.get();
      } else {
        tokenAncestors =
          longestSharedPrefix(ancestorsOfLeftVisible.get(), ancestorsOfRightVisible.get());
      }
    }

    return (a, b) -> {
      return -Integer.compare(
        sharedPrefixLength(tokenAncestors, ancestorsExcludingSubTree(a, b)),
        sharedPrefixLength(tokenAncestors, ancestorsExcludingSubTree(b, a))
      );
    };
  }

  private boolean inside(Range range, int cursor) {
    // cursor is at start or end of a list, or between elements.
    return range.start <= cursor && cursor <= range.stop;
  }

  private List<AstNode> longestSharedPrefix(List<AstNode> a, List<AstNode> b) {
    int index = 0;
    int n = Math.min(a.size(), b.size());
    for (; index < n; index++) {
      if (!a.get(index).equals(b.get(index))) {
        break;
      }
    }
    return a.subList(0, index);
  }

  public <T extends AstList<?>> T closestList(
    AstParent root,
    String source,
    Stream<T> lists,
    int cursor
  ) {
    try {
      return closestNode(
        root,
        source,
        listsToPrioritize(source, cursor, lists.collect(Collectors.toList())).stream(),
        cursor
      );
    } catch (ObjectNotFound e) {
      throw new CannotFindInsertionPoint();
    }
  }

  public <T extends AstNode> T closestNode(
    AstParent root,
    String source,
    Stream<T> nodes,
    int cursor
  ) {
    return nodes
      .min(closestNodeComparator(root, source, cursor))
      .orElseThrow(() -> new ObjectNotFound());
  }

  public Range closestRange(String source, int cursor, List<Range> ranges) {
    return ranges
      .stream()
      .min(closestRangeComparator(source, cursor))
      .orElseThrow(() -> new ObjectNotFound());
  }

  public boolean isReasonableDistance(String source, int cursor, Range range, int threshold) {
    LinePositionConverter converter = new LinePositionConverter(source);
    int cursorLine = converter.position(cursor);
    if (
      Math.abs(converter.position(range.start) - cursorLine) > threshold ||
      Math.abs(converter.position(range.stop) - cursorLine) > threshold
    ) {
      return false;
    }

    return true;
  }
}
