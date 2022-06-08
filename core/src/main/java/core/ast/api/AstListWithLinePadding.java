package core.ast.api;

import core.ast.Ast;
import core.util.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AstListWithLinePadding<T extends AstParent> extends AstList<T> {

  private List<Range> contiguousBlankLines() {
    // line ranges of the blank line blocks in the attributable range.
    List<Range> tokenLines = tree().whitespace.lineRanges(tokenRangeWithCommentsAndWhitespace());
    List<Range> blankTokenRanges = new ArrayList<>();
    for (Range tokenLine : tokenLines) {
      if (
        tokens()
          .subList(tokenLine.start, tokenLine.stop)
          .stream()
          .allMatch(tree().whitespace::isWhitespace)
      ) {
        int lastIndex = blankTokenRanges.size() - 1;
        if (lastIndex >= 0 && blankTokenRanges.get(lastIndex).stop == tokenLine.start) {
          blankTokenRanges.get(lastIndex).stop = tokenLine.stop;
        } else {
          blankTokenRanges.add(tokenLine);
        }
      }
    }

    // We don't want the line after the newline in the line ranges.
    return blankTokenRanges
      .stream()
      .map(tokenRange -> tree().whitespace.lineRange(tokenRange))
      .map(lineRange -> new Range(lineRange.start, Math.max(lineRange.start, lineRange.stop - 1)))
      .collect(Collectors.toList());
  }

  private int elementLineStart(int elementIndex) {
    return tree().whitespace.lineStart(elements().get(elementIndex).tokenRange().get().start);
  }

  private int elementLineEnd(int elementIndex) {
    return tree().whitespace.lineEnd(elements().get(elementIndex).tokenRange().get().stop);
  }

  private List<AstNode> elementsAndStandaloneComments() {
    List<AstNode> ret = new ArrayList<>();
    Range range = tokenRangeWithCommentsAndWhitespace();
    List<Range> elementRanges = elements()
      .stream()
      .map(e -> e.tokenRange().get())
      .collect(Collectors.toList());
    List<Ast.Comment> comments = tree()
      .comments.stream()
      .filter(
        c ->
          range.start <= c.tokenRange().get().start &&
          c.tokenRange().get().stop <= range.stop &&
          !elementRanges
            .stream()
            .filter(
              r -> r.start <= c.tokenRange().get().start && c.tokenRange().get().stop <= r.stop
            )
            .findFirst()
            .isPresent()
      )
      .collect(Collectors.toList());
    ret.addAll(elements());
    ret.addAll(comments);

    Map<AstNode, Range> ranges = ret
      .stream()
      .collect(Collectors.toMap(e -> e, e -> e.tokenRange().get()));
    Collections.sort(ret, Comparator.comparingInt(e -> ranges.get(e).start));
    return ret;
  }

  private void ensureMinimumSeparation(AstNode left, AstNode right, boolean isComment) {
    if (!isComment && (!canAdd(left) || !canAdd(right))) {
      return;
    }

    // Calculate minimum number of blank lines.
    int minimumBlankLines = Math.max(minimumBlankLines(left), minimumBlankLines(right));

    // Add blank lines to ensure minimum.
    int numBlankLines =
      tree().whitespace.lineRange(right.tokenRange().get()).start -
      tree().whitespace.lineRange(left.tokenRange().get()).stop;
    int elementLineStart = tree().whitespace.lineStart(right.tokenRange().get().start);
    for (int i = 0; i < minimumBlankLines - numBlankLines; i++) {
      tokens().add(elementLineStart, tree().factory.createNewline());
    }
  }

  private void removeLeadingBlankLines(int elementIndex, int maxLinesToRemove) {
    int start = elements().get(elementIndex).lineRange().get().start;
    Range removalLineRange = contiguousBlankLines()
      .stream()
      .filter(r -> r.stop == start)
      .findFirst()
      .orElse(new Range(0, 0));
    removalLineRange =
      new Range(
        Math.max(removalLineRange.start, removalLineRange.stop - maxLinesToRemove),
        removalLineRange.stop
      );
    Range removalTokenRange = tree().whitespace.lineRangeToTokenRange(removalLineRange);
    tokens().subList(removalTokenRange.start, removalTokenRange.stop).clear();
  }

  private void removeTrailingBlankLines(int elementIndex, int maxLinesToRemove) {
    int stop = elements().get(elementIndex).lineRange().get().stop;
    Range removalLineRange = contiguousBlankLines()
      .stream()
      .filter(r -> r.start == stop)
      .findFirst()
      .orElse(new Range(0, 0));
    removalLineRange =
      new Range(
        removalLineRange.start,
        Math.min(removalLineRange.stop, removalLineRange.start + maxLinesToRemove)
      );
    Range removalTokenRange = tree().whitespace.lineRangeToTokenRange(removalLineRange);
    tokens().subList(removalTokenRange.start, removalTokenRange.stop).clear();
  }

  protected int innerMinimumBlankLines(AstNode innerElement) {
    return 0;
  }

  protected int minimumBlankLines(AstNode element) {
    // Don't override this.
    return Math.max(
      element.children().stream().findFirst().map(e -> innerMinimumBlankLines(e)).orElse(0),
      wrapperMinimumBlankLines(element)
    );
  }

  protected int wrapperMinimumBlankLines(AstNode wrapperElement) {
    return 0;
  }

  public void addAtLine(int line, AstParent e) {
    super.addAtLine(line, e);
    AstParent addedElement = e.parent().filter(p -> elementType().isInstance(p)).orElse(e);

    List<AstNode> elementsAndStandaloneComments = elementsAndStandaloneComments();
    int index = elementsAndStandaloneComments.indexOf(addedElement);
    if (index > 0) {
      boolean isComment = addedElement instanceof Ast.Comment ? true : false;
      ensureMinimumSeparation(
        elementsAndStandaloneComments.get(index - 1),
        elementsAndStandaloneComments.get(index),
        isComment
      );
    }

    if (index + 1 < elementsAndStandaloneComments.size()) {
      ensureMinimumSeparation(
        elementsAndStandaloneComments.get(index),
        elementsAndStandaloneComments.get(index + 1),
        false
      );
    }
  }

  public String indent() {
    return indentationUtils.indent(this);
  }

  @Override
  public boolean isMultiline() {
    return true;
  }

  @Override
  public void remove(AstNode element) {
    if (element instanceof Ast.Comment) {
      super.remove(element);
      return;
    }

    int maxLinesToDelete = minimumBlankLines(element);
    int elementIndex = elements().indexOf(element);
    if (elementIndex != elements().size() - 1) {
      removeTrailingBlankLines(elementIndex, maxLinesToDelete);
    }
    if (elementIndex != 0) {
      removeLeadingBlankLines(elementIndex, maxLinesToDelete);
    }

    super.remove(element);
    if (elementIndex > 0 && elementIndex < elements().size()) {
      ensureMinimumSeparation(
        elements().get(elementIndex - 1),
        elements().get(elementIndex),
        false
      );
    }
  }
}
