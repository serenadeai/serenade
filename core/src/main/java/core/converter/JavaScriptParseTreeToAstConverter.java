package core.converter;

import core.ast.Ast;
import core.ast.JavaScriptAst;
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
public class JavaScriptParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public JavaScriptParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_JAVASCRIPT;
  }

  @Override
  protected void registerConverters() {
    register("return_type_optional", () -> new JavaScriptAst.ReturnTypeOptional());
    register("import", () -> new JavaScriptAst.Import());
    register("object", () -> new Ast.Dictionary());

    // See comment in base converter.
    register(
      "program",
      () -> new Ast.Program(),
      fixer ->
        fixer.wrap(
          parent ->
            !parent
              .children()
              .stream()
              .anyMatch(child -> child instanceof JavaScriptAst.StatementList),
          () -> new JavaScriptAst.StatementList()
        )
    );
    register("statement_list", () -> new JavaScriptAst.StatementList());
    register("type_optional", () -> new JavaScriptAst.TypeOptional());

    register(
      "method",
      () -> new Ast.Method(),
      fixer ->
        fixer.setType(
          parent ->
            parent
              .children()
              .stream()
              .anyMatch(
                child ->
                  child.getClass().equals(Ast.Identifier.class) &&
                  child.children().stream().anyMatch(token -> token.isToken("constructor"))
              ),
          () -> new Ast.Constructor()
        )
    );

    // Sometimes we need to additionally inline whitespace nodes.
    register(
      "jsx_text",
      () -> new Inlined(),
      fixer ->
        fixer.inline(
          node -> node.getClass().equals(AstToken.class) && whitespace.isWhitespace(node.code())
        )
    );

    // Sometimes we get empty markup content nodes.
    register(
      "markup_content",
      () -> new Ast.MarkupContent(),
      fixer -> fixer.setType(parent -> parent.children().size() == 0, () -> new Inlined())
    );
  }

  @Override
  protected boolean bracesOnSameLine() {
    return true;
  }
}
