package core.selector;

import core.ast.Ast;
import core.ast.DartAst;
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
public class DartSelectorMap extends SelectorMap {

  @Inject
  public DartSelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_DART;
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    m.put(ObjectType.GETTER, instanceMatch(DartAst.Getter.class));
    m.put(ObjectType.OPERATOR, instanceMatch(DartAst.Operator.class));
    m.put(ObjectType.MIXIN, instanceMatch(DartAst.MixinType.class));
    m.put(ObjectType.NAMED_PARAMETER, filterByParent(instanceMatch(DartAst.NamedParameter.class)));
    m.put(
      ObjectType.NAMED_PARAMETER_LIST,
      filterByParent(instanceMatch(DartAst.NamedParameterList.class))
    );
    m.put(
      ObjectType.POSITIONAL_PARAMETER,
      filterByParent(instanceMatch(DartAst.PositionalParameter.class))
    );
    m.put(
      ObjectType.POSITIONAL_PARAMETER_LIST,
      filterByParent(instanceMatch(DartAst.PositionalParameterList.class))
    );
    m.put(ObjectType.SETTER, instanceMatch(DartAst.Setter.class));
    m.put(ObjectType.WITH, instanceMatch(DartAst.MixinType.class));
    m.put(ObjectType.WITH_LIST, instanceMatch(DartAst.MixinList.class));

    return applyIndexing(m);
  }
}
