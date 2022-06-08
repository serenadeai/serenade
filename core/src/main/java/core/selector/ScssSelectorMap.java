package core.selector;

import core.ast.Ast;
import core.ast.ScssAst;
import core.ast.api.AstNode;
import core.gen.rpc.Language;
import core.util.ObjectType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ScssSelectorMap extends SelectorMap {

  @Inject
  public ScssSelectorMap() {
    initialize();
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> defaultAstSelectors() {
    return new HashMap<>();
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    m.put(ObjectType.ARGUMENT, filterByParent(instanceMatch(Ast.Argument.class)));
    m.put(ObjectType.ARGUMENT_LIST, instanceMatch(Ast.ArgumentList.class));
    m.put(ObjectType.MIXIN, instanceMatch(Ast.CssMixin.class));
    m.put(ObjectType.NAME, instanceMatch(Ast.Identifier.class));
    m.put(ObjectType.PARAMETER, filterByParent(instanceMatch(Ast.Parameter.class)));
    m.put(ObjectType.PARAMETER_LIST, instanceMatch(Ast.ParameterList.class));
    m.put(ObjectType.PROPERTY, instanceMatch(Ast.KeyValuePair.class));
    m.put(ObjectType.RULESET, instanceMatch(Ast.CssRuleset.class));
    m.put(ObjectType.VALUE, instanceMatch(Ast.KeyValuePairValue.class));

    return applyIndexing(m);
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_SCSS;
  }
}
