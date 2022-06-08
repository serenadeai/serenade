package core.selector;

import core.ast.Ast;
import core.ast.JavaScriptAst;
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
public class JavaScriptSelectorMap extends SelectorMap {

  @Inject
  public JavaScriptSelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_JAVASCRIPT;
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    m.put(
      ObjectType.ASSERT,
      predicateMatch(
        p -> {
          if (p instanceof Ast.Statement) {
            AstNode expression = ((Ast.Statement) p).children().get(0);
            return (
              (expression instanceof Ast.Call) &&
              (((Ast.Call) expression).nameString().equals("assert"))
            );
          }
          return false;
        }
      )
    );
    m.put(ObjectType.OBJECT, instanceMatch(Ast.Dictionary.class));
    m.put(ObjectType.RETURN_TYPE, returnTypeSelectorForPythonAndJavascript());

    m = filterByName(m);
    m = applyIndexing(m);
    m.putAll(htmlAstSelectors());
    return m;
  }
}
