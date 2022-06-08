package core.ast.api;

import java.util.List;
import java.util.Optional;

public interface AstParent extends AstNode {
  void propagateOrFinalizeRemoval(AstNode child);
  void removeSpacingForChildBecomingInvisible(AstNode child);
  void setChildren(List<AstNode> children);
  int setupSpacingForChildBecomingVisible(AstNode child);
}
