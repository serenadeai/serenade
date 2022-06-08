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
public class DefaultSelectorMap extends SelectorMap {

  @Inject
  public DefaultSelectorMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_DEFAULT;
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    return Collections.emptyMap();
  }
}
