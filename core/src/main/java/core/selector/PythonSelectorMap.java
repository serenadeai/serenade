package core.selector;

import core.ast.Ast;
import core.ast.PythonAst;
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
public class PythonSelectorMap extends SelectorMap {

  @Inject
  public PythonSelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_PYTHON;
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    m.put(ObjectType.GENERATOR, instanceMatch(Ast.Generator.class));
    m.put(ObjectType.PASS, instanceMatch(Ast.Placeholder.class));
    m.put(ObjectType.RETURN_TYPE, returnTypeSelectorForPythonAndJavascript());

    return applyIndexing(m);
  }
}
