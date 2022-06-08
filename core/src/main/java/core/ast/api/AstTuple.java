package core.ast.api;

import core.util.Range;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AstTuple extends DefaultAstParent implements AstContainer {

  public abstract String containerType();

  protected abstract String delimiter();

  public List<AstNode> elements() {
    return children()
      .stream()
      .filter(child -> !(child instanceof AstToken) || !(child.code().equals(delimiter())))
      .collect(Collectors.toList());
  }

  @Override
  public int setupSpacingForChildBecomingVisible(AstNode child) {
    int childIndex = children().indexOf(child);
    int index = elements().indexOf(child);
    AstToken delimiter = tree().factory.createToken(delimiter());

    //put delimiter to seperate non-empty siblings
    Optional<AstToken> leftToken = child
      .rightMostLeft(AstToken.class)
      .filter(e -> e.ancestors().filter(a -> a.equals(this)).findFirst().isPresent());
    Optional<AstToken> rightToken = child
      .leftMostRight(AstToken.class)
      .filter(e -> e.ancestors().filter(a -> a.equals(this)).findFirst().isPresent());
    if (leftToken.isPresent()) {
      tokens()
        .addAll(
          tokens().indexOf(leftToken.get()) + 1,
          Arrays.asList(delimiter, tree().factory.createToken(" "))
        );
      addChild(childIndex, delimiter);
    } else if (rightToken.isPresent()) {
      tokens()
        .addAll(
          tokens().indexOf(rightToken.get()),
          Arrays.asList(delimiter, tree().factory.createToken(" "))
        );
      addChild(childIndex + 1, delimiter);
    }
    return child.tokenRangeWithCommentsAndWhitespace().stop;
  }

  @Override
  public void removeSpacingForChildBecomingInvisible(AstNode child) {
    int childIndex = children().indexOf(child);
    int index = elements().indexOf(child);

    Optional<AstToken> leftToken = child
      .rightMostLeft(AstToken.class)
      .filter(e -> e.ancestors().filter(a -> a.equals(this)).findFirst().isPresent());
    Optional<AstToken> rightToken = child
      .leftMostRight(AstToken.class)
      .filter(e -> e.ancestors().filter(a -> a.equals(this)).findFirst().isPresent());
    if (leftToken.isPresent()) {
      Range leftTokenRange = leftToken.get().tokenRangeWithCommentsAndWhitespace();
      tokens().removeAll(tokens().subList(leftTokenRange.start, leftTokenRange.stop));
      children.get(childIndex - 1).children().remove(leftToken.get());
    } else if (rightToken.isPresent()) {
      Range rightTokenRange = rightToken.get().tokenRangeWithCommentsAndWhitespace();
      tokens().removeAll(tokens().subList(rightTokenRange.start, rightTokenRange.stop));
      children.get(childIndex + 1).children().remove(rightToken.get());
    }
  }
}
