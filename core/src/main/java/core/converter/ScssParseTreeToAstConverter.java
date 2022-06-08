package core.converter;

import core.ast.Ast;
import core.ast.ScssAst;
import core.ast.api.AstNode;
import core.ast.api.AstToken;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ScssParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public ScssParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_SCSS;
  }

  @Override
  protected void registerConverters() {
    register("statement_list", () -> new ScssAst.StatementList());
  }
}
