package core.selector;

import core.ast.Ast;
import core.ast.api.AstNode;
import core.gen.rpc.Language;
import core.util.ObjectType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GoSelectorMap extends SelectorMap {

  @Inject
  public GoSelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_GO;
  }

  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    m.put(ObjectType.METHOD, goMethodSelector());
    m.put(ObjectType.TAG, instanceMatch(Ast.PropertyTag.class));

    return applyIndexing(m);
  }
}
