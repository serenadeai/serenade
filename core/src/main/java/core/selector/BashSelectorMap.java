package core.selector;

import core.ast.Ast;
import core.ast.BashAst;
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
public class BashSelectorMap extends SelectorMap {

  @Inject
  public BashSelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_BASH;
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    // We expand this to include the `fi` and not only the IfClause, so that deletions don't leave
    // the `fi` trailing.
    m.put(ObjectType.IF, instanceMatch(Ast.If.class));
    return m;
  }
}
