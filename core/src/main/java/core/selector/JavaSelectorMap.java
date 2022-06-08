package core.selector;

import core.ast.Ast;
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
public class JavaSelectorMap extends SelectorMap {

  @Inject
  public JavaSelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_JAVA;
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    m.put(ObjectType.FUNCTION, instanceMatch(Ast.Method.class));

    m = filterByName(m);
    m = applyIndexing(m);
    return m;
  }
}
