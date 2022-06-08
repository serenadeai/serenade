package core.selector;

import core.ast.Ast;
import core.ast.api.AstNode;
import core.gen.rpc.Language;
import core.util.ObjectType;
import core.util.ObjectType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KotlinSelectorMap extends SelectorMap {

  @Inject
  public KotlinSelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_KOTLIN;
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    m.put(
      ObjectType.PARENT,
      applyIndexing(filterByParent(instanceMatch(Ast.ImplementsType.class)))
    );
    m.put(ObjectType.PARENT_LIST, instanceMatch(Ast.ImplementsList.class));
    return m;
  }
}
