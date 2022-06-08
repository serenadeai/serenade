package core.ast.api;

import core.util.Range;
import java.util.List;
import javax.inject.Inject;

public class IndentationUtils {

  @Inject
  public IndentationUtils() {}

  public void decreaseIndentation(AstNode node, String decrease) {
    Range range = node.tokenRangeWithCommentsAndWhitespace();
    List<AstToken> tokens = node.tokens();
    for (int i = range.start; i < range.stop; i++) {
      if (i == 0 || tokens.get(i - 1) instanceof AstNewline) {
        if (node.tree().whitespace.isWhitespace(tokens.get(i))) {
          String indent = tokens.get(i).code;
          if (indent.startsWith(decrease)) {
            indent = indent.substring(decrease.length());
          }
          tokens.remove(i);
          tokens.add(i, node.tree().factory.createToken(indent));
        }
      }
    }
  }

  public void increaseIndentation(AstNode node, String increase) {
    Range range = node.tokenRangeWithCommentsAndWhitespace();
    List<AstToken> tokens = node.tokens();
    for (int i = range.start; i < range.stop; i++) {
      if (i == 0 || tokens.get(i - 1) instanceof AstNewline) {
        String indent = increase;
        if (node.tree().whitespace.isWhitespace(tokens.get(i))) {
          indent = tokens.get(i).code + increase;
          tokens.remove(i);
        }

        tokens.add(i, node.tree().factory.createToken(indent));
      }
    }
  }

  public String indent(AstNode node) {
    // current indentation. not necessarily a node in the tree.
    return node.ancestor(AstIndentAligner.class).map(AstIndentAligner::indent).orElse("");
  }

  public String parentIndent(AstNode node) {
    return node
      .ancestors(AstIndentAligner.class)
      .skip(1)
      .findFirst()
      .map(AstIndentAligner::indent)
      .orElse("");
  }
}
