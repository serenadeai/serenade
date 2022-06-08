package core.ast.api;

import java.util.List;
import java.util.Optional;

public abstract class AstListOptional extends AstOptional<AstList> {

  public abstract List<Class<? extends AstNode>> innerElementTypes();

  public boolean isInnerElementType(Class<? extends AstNode> nodeType) {
    return innerElementTypes().stream().anyMatch(t -> t.isAssignableFrom(nodeType));
  }

  @Override
  public int setupSpacingForChildBecomingVisible(AstNode child) {
    // a bit hacky, but our prefix tokens include the whitespace separator,
    // so use the index to the right of it.
    return child.tokenRangeWithCommentsAndWhitespace().start;
  }
}
