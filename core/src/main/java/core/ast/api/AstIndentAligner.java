package core.ast.api;

import core.util.Range;
import core.util.Whitespace;
import java.util.Optional;
import java.util.stream.Collectors;

public class AstIndentAligner extends DefaultAstParent {

  private Whitespace whitespace = new Whitespace();

  protected String defaultIndentationToken() {
    return whitespace.indentationToken(tree.root.codeWithCommentsAndWhitespace(), 2);
  }

  public String indent() {
    AstNode innerChild = children().size() > 1 ? children().get(1) : children().get(0);
    Optional<Range> tokenRange = innerChild.tokenRange();
    if (tokenRange.isPresent()) {
      int stop = tokenRange.get().start;
      int lineStart = tree().whitespace.lineStart(stop);
      if (lineStart >= innerChild.tokenRangeWithCommentsAndWhitespace().start) {
        Range range = new Range(lineStart, stop);
        String ret = tokens()
          .subList(range.start, range.stop)
          .stream()
          .map(t -> t.code)
          .collect(Collectors.joining(""));

        return ret;
      }
    }

    return (
      ancestor(AstIndentAligner.class).map(AstIndentAligner::indent).orElse("") +
      defaultIndentationToken()
    );
  }
}
