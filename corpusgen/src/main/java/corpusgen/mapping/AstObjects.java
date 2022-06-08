package corpusgen.mapping;

import core.ast.Ast;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.codeengine.Tokenizer;
import core.util.Range;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AstObjects {

  public final Set<Integer> textPositions;
  public final Map<Integer, List<AstNode>> positionToObjects;
  public final AstParent root;

  public AstObjects(AstParent root) {
    this.root = root;
    this.positionToObjects =
      Stream
        .concat(
          root.find(AstNode.class),
          root.tree().comments.stream().flatMap(e -> e.find(AstNode.class))
        )
        .filter(
          e ->
            e instanceof Ast.If ||
            e instanceof Ast.Lambda ||
            e instanceof Ast.MarkupElement ||
            e instanceof Ast.MarkupOpeningTag ||
            e instanceof Ast.MarkupClosingTag ||
            e instanceof Ast.MarkupSingletonTag
        )
        .collect(
          Collectors.groupingBy(
            e -> e.range().start,
            Collectors.mapping(e -> e, Collectors.toList())
          )
        );
    textPositions = new HashSet<>();
    Stream
      .concat(root.tree().comments.stream(), root.find(Ast.String_.class))
      .forEach(
        s -> {
          Range range = s.range();
          for (int i = range.start; i < range.stop; i++) {
            textPositions.add(i);
          }
        }
      );
  }

  public List<AstNode> eligibleObjects(List<Tokenizer.Token> tokens, int index) {
    if (!(tokens.get(index) instanceof Tokenizer.CodeToken)) {
      return Collections.emptyList();
    }
    return Optional
      .ofNullable(positionToObjects.get(((Tokenizer.CodeToken) tokens.get(index)).range.start))
      .orElse(Collections.emptyList());
  }
}
