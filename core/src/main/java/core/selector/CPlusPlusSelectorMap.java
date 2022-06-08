package core.selector;

import core.ast.Ast;
import core.ast.CPlusPlusAst;
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
public class CPlusPlusSelectorMap extends SelectorMap {

  @Inject
  public CPlusPlusSelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_CPLUSPLUS;
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    return m;
  }
}
