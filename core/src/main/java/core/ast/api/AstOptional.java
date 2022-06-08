package core.ast.api;

import com.google.common.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AstOptional<T extends AstNode>
  extends DefaultAstParent
  implements AstContainer {

  public abstract String containerType();

  public abstract Class<? extends T> elementType();

  public void clear() {
    optional().ifPresent(e -> e.remove());
  }

  public boolean canAdd(AstNode element) {
    return elementType().isInstance(element);
  }

  public boolean canAdd(Class<? extends AstNode> node) {
    return elementType().isAssignableFrom(node);
  }

  public Optional<T> optional() {
    return children()
      .stream()
      .filter(e -> elementType().isInstance(e))
      .map(e -> (T) elementType().cast(e))
      .findFirst();
  }

  protected String prefix() {
    return "";
  }

  protected String postfix() {
    return "";
  }

  protected void prepareForAdd(AstNode element) {
    // Modify the element we are about to add, so that it will fit with the tree. For example,
    // in python we increase the indentations in the subtree.
  }

  @Override
  public void remove() {
    assert false : "Cannot remove parent optional, remove child instead";
  }

  public void set(AstNode element) {
    int tokenIndex = setupSpacingBeforeBecomingVisible();
    tokenRange().ifPresent(range -> tokens().removeAll(tokens().subList(range.start, range.stop)));
    children().clear();

    prepareForAdd(element);

    AstToken prefix = tree().factory.createToken(prefix());
    AstToken postfix = tree().factory.createToken(postfix());

    List<AstToken> newTokens = new ArrayList<>();
    newTokens.add(prefix);
    newTokens.addAll(element.tokens());
    newTokens.add(postfix);
    tokens().addAll(tokenIndex, newTokens);

    addChild(prefix);
    addChild(element);
    addChild(postfix);

    forEach(e -> e.setTree(tree()));
  }

  @Override
  public void propagateOrFinalizeRemoval(AstNode child) {
    removeSpacingBeforeBecomingInvisible();
    tokens().removeAll(tokensInVisibleRange());
    removeAllChildren();
  }
}
