package core.selector;

import core.ast.Ast;
import core.ast.CSharpAst;
import core.ast.api.AstNode;
import core.gen.rpc.Language;
import core.util.ObjectType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RubySelectorMap extends SelectorMap {

  @Inject
  public RubySelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_RUBY;
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    m.put(
      ObjectType.CONSTRUCTOR,
      predicateMatch(
        node ->
          node instanceof Ast.Constructor ||
          (node instanceof Ast.Method && node.nameString().equals("initialize"))
      )
    );
    m.put(ObjectType.HASH, instanceMatch(Ast.Dictionary.class));
    m.put(ObjectType.MODULE, instanceMatch(Ast.Namespace.class));
    m.put(ObjectType.REQUIRE, instanceMatch(Ast.Import.class));
    return m;
  }
}
