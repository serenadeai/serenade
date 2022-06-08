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
public class HtmlSelectorMap extends SelectorMap {

  @Inject
  public HtmlSelectorMap() {
    initialize();
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> defaultAstSelectors() {
    return new HashMap<>();
  }

  @Override
  protected Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> languageAstSelectors() {
    Map<ObjectType, Function<AstSelectionContext, Stream<AstNode>>> m = new HashMap<>();
    // There's no key/value in html, so alias it to attribute value.
    m.put(ObjectType.KEY, instanceMatch(Ast.MarkupAttributeName.class));
    m.put(ObjectType.VALUE, instanceMatch(Ast.MarkupAttributeValue.class));
    m.put(ObjectType.STRING, instanceMatch(Ast.String_.class));
    m.put(ObjectType.STRING_TEXT, instanceMatch(Ast.StringText.class));
    m.put(ObjectType.ATTRIBUTE_TEXT, attributeTextSelector());
    m = filterByName(m);
    m = applyIndexing(m);
    m.putAll(htmlAstSelectors());
    return m;
  }

  protected Function<AstSelectionContext, Stream<AstNode>> attributeTextSelector() {
    return predicateMatch(
      p -> p instanceof Ast.StringText && p.ancestor(Ast.MarkupAttributeValue.class).isPresent()
    );
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_HTML;
  }
}
