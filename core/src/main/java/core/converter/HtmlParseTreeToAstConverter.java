package core.converter;

import core.ast.Ast;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HtmlParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public HtmlParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_HTML;
  }

  @Override
  protected void registerConverters() {
    register("quoted_attribute_value", this::convertString);

    // The text comes back tokenized line-by-line, so we wrap each AstToken individually.
    register(
      Arrays.asList("raw_text", "text_"),
      () -> new Inlined(),
      fixer -> fixer.wrapIndividually(matches(AstToken.class), () -> new Ast.MarkupText())
    );

    // MarkupText nodes might show up as consecutive nodes, so we need to wrap them with MarkupContent separately.
    register(
      "markup_content",
      () -> new Ast.MarkupContent(),
      fixer ->
        fixer
          .setType(parent -> parent.children().size() == 0, () -> new Inlined())
          .setType(
            parent -> parent.children().stream().anyMatch(matches(Ast.MarkupText.class)),
            () -> new Inlined()
          )
          .wrapIndividually(matches(Ast.MarkupText.class), () -> new Ast.MarkupContent())
    );

    register(
      "fragment",
      () -> new Ast.MarkupContentList(),
      fixer -> fixer.inline(matches(DefaultAstParent.class))
    );
  }
}
