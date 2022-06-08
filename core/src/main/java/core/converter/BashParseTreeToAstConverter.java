package core.converter;

import core.ast.Ast;
import core.ast.BashAst;
import core.ast.api.AstList;
import core.ast.api.AstListOptional;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import toolbelt.languages.LanguageDeterminer;

@Singleton
public class BashParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public BashParseTreeToAstConverter(LanguageDeterminer languageDeterminer) {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_BASH;
  }

  @Override
  protected void registerConverters() {
    register("argument_list", () -> new BashAst.ArgumentList());
    // The last statement is parsed differently in the grammar, and cannot be easily simplified.
    register("last_statement", () -> new Ast.Statement());
  }
}
