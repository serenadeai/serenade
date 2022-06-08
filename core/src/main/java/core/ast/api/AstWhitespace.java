package core.ast.api;

import core.util.Range;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// We need to reduce repeated code with Whitespace
public class AstWhitespace {

  protected Whitespace whitespace = new Whitespace();
  private List<AstToken> tokens;

  public AstWhitespace(List<AstToken> tokens) {
    this.tokens = tokens;
  }

  public int expandLeftOnLine(int position) {
    position--;
    while (
      position >= 0 &&
      !(tokens.get(position) instanceof AstNewline) &&
      isWhitespace(tokens.get(position))
    ) {
      position--;
    }

    return position + 1;
  }

  public int expandRightOnLine(int position) {
    while (
      position < tokens.size() &&
      !(tokens.get(position) instanceof AstNewline) &&
      isWhitespace(tokens.get(position))
    ) {
      position++;
    }

    return position < 0 ? tokens.size() : position;
  }

  public Range lineRange(Range tokenRange) {
    if (tokens.size() == 0) {
      return new Range(0);
    }
    int lineStop = 1;
    if (tokenRange.stop > 0) {
      // We might not have a token at tokenRange.stop to call priorNewlines
      // on, so use the previous token and then add one if using the previous one
      // lowered the count.
      AstToken beforeStop = tokens.get(tokenRange.stop - 1);
      lineStop = beforeStop.priorNewlines + 1;
      if (beforeStop instanceof AstNewline) {
        lineStop++;
      }
    }
    return new Range(tokens.get(tokenRange.start).priorNewlines, lineStop);
  }

  public int lineFromTokenIndex(int tokenIndex) {
    return tokens.get(tokenIndex).priorNewlines;
  }

  public Range lineTokenRange(int line) {
    int lineStart = line == 0
      ? 0
      : tokens
        .stream()
        .filter(t -> t.priorNewlines == line - 1 && t instanceof AstNewline)
        .findFirst()
        .map(t -> tokens.indexOf(t) + 1)
        .get();

    return new Range(lineStart, lineEnd(lineStart));
  }

  public Range lineRangeToTokenRange(Range lineRange) {
    int start = lineTokenRange(lineRange.start).start;
    if (lineRange.start == lineRange.stop) {
      return new Range(start, start);
    }
    return new Range(start, lineTokenRange(lineRange.stop - 1).stop + 1);
  }

  public boolean isWhitespace(AstNode node) {
    return whitespace.isWhitespace(node.code());
  }

  public boolean isWhitespace(int line) {
    Range lineTokenRange = lineTokenRange(line);

    for (int i = lineTokenRange.start; i < lineTokenRange.stop; i++) {
      if (!isWhitespace(tokens.get(i))) {
        return false;
      }
    }

    return true;
  }

  public int lineStart(int position) {
    // note that because of the -1 return value, this works on the first line of a file.
    return previousNewline(position) + 1;
  }

  public int lineEnd(int position) {
    int ret = nextNewline(position);
    if (ret < 0) {
      return tokens.size();
    }

    return ret;
  }

  public int previousNewline(int position) {
    position--;
    while (position >= 0 && !(tokens.get(position) instanceof AstNewline)) {
      position--;
    }

    return position;
  }

  public int nextNewline(int position) {
    while (position < tokens.size() && !(tokens.get(position) instanceof AstNewline)) {
      position++;
    }

    return position;
  }

  public List<Range> lineRanges(Range eligible) {
    // token ranges of the full lines in the attributable range. Includes newline at the of the
    // line.
    while (
      eligible.start - 1 < tokens.size() &&
      !(eligible.start == 0 || tokens.get(eligible.start - 1) instanceof AstNewline)
    ) {
      eligible.start++;
    }

    if (eligible.start == tokens.size()) {
      return Collections.emptyList();
    }

    List<Range> ranges = new ArrayList<>();
    int nextStart = eligible.start;
    for (int i = eligible.start; i < eligible.stop; i++) {
      if (tokens.get(i) instanceof AstNewline) {
        ranges.add(new Range(nextStart, i + 1));
        nextStart = i + 1;
      }
    }

    return ranges;
  }

  public Range includeLineWhitespaceOnOneSide(Range initRemovalRange) {
    Range removalRange = new Range(initRemovalRange.start, initRemovalRange.stop);
    // try to strip left, else try to strip right. stop at newlines.
    int leftSpacingStart = removalRange.start;
    for (
      ;
      leftSpacingStart > 0 && isWhitespace(tokens.get(leftSpacingStart - 1));
      leftSpacingStart--
    ) {}
    removalRange.start = Math.max(leftSpacingStart, lineStart(removalRange.start) - 1);
    if (removalRange.start == initRemovalRange.start) {
      int rightSpacingStop = removalRange.stop;
      for (
        ;
        rightSpacingStop < tokens.size() && isWhitespace(tokens.get(rightSpacingStop));
        rightSpacingStop++
      ) {}
      removalRange.stop = Math.min(rightSpacingStop, lineEnd(removalRange.stop) + 1);
    }
    return removalRange;
  }

  public int lineCount() {
    if (tokens.size() == 0) {
      return 1;
    }

    return tokens.get(tokens.size() - 1).priorNewlines + 1;
  }
}
