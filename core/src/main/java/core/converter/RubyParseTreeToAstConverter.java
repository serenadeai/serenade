package core.converter;

import core.ast.Ast;
import core.ast.CSharpAst;
import core.ast.RubyAst;
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

@Singleton
public class RubyParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public RubyParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_RUBY;
  }

  @Override
  protected void registerConverters() {
    register("module", () -> new Ast.Namespace());
    register("statement_list", () -> new RubyAst.StatementList());
    register("enclosed_body_", () -> new Ast.EnclosedBody());

    // Need to do this as we cannot insert nested optional nodes directly.
    register(
      "body_and_statement_list",
      () -> new Ast.EnclosedBody(),
      fixer ->
        fixer.inline(matches(RubyAst.StatementList.class)).wrap(() -> new RubyAst.StatementList())
    );

    // empty statement needs to be its own node or it breaks ruby grammar
    register("empty_statement", () -> new Ast.Statement());

    register(
      "statement",
      () -> new Ast.Statement(),
      fixer -> fixer.changeChildType(() -> new Ast.Method(), () -> new Ast.Function())
    );

    register(
      "class_member_list",
      () -> new Ast.EnclosedBody(),
      fixer ->
        fixer.inline(matches(Ast.ClassMemberList.class)).wrap(() -> new Ast.ClassMemberList())
    );

    register("extends_optional", () -> new RubyAst.ExtendsOptional());
    register("raise", () -> new Ast.Throw());
    register("include", () -> new Ast.Import());
    register("require", () -> new Ast.Import());
    register("require_relative", () -> new Ast.Import());
    register("parameter_list_optional", () -> new RubyAst.ParameterListOptional());
  }
}
