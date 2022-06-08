package core.converter;

import core.ast.Ast;
import core.ast.CSharpAst;
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
public class CSharpParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public CSharpParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_CSHARP;
  }

  @Override
  protected void registerConverters() {
    register("catch_parameter_optional", () -> new CSharpAst.CatchParameterOptional());
    register("declaration_member_list", () -> new CSharpAst.DeclarationMemberList());
    register("directives_list", () -> new CSharpAst.DirectivesList());
    register("extends_list_optional", () -> new CSharpAst.ExtendsListOptional());
    register("initializer_expression_list", () -> new CSharpAst.InitializerExpressionList());
    register("local_function_statement", () -> new Ast.Function());
    register(
      "parameter",
      () -> new Ast.Parameter(),
      fixer ->
        fixer.changeChildType(() -> new Ast.AssignmentValue(), () -> new Ast.ParameterValue())
    );
    register(
      "type_parameter_constraint_list_optional",
      () -> new CSharpAst.TypeParameterConstraintListOptional()
    );
    register("using_statement", () -> new CSharpAst.UsingBlock());

    // Avoiding renaming attributes to decorators in the grammar.
    register("attribute_list_placeholder", () -> new Ast.DecoratorList());
    register("attribute_list", () -> new Ast.Decorator());
    register("attribute_body", () -> new Ast.DecoratorExpression());

    // Not renaming field to property, because they're distinct concepts in C#.
    register("field_declaration", () -> new Ast.Property());
    register("property_declaration", () -> new Ast.Property());
    register("constructor_declaration", () -> new Ast.Constructor());

    // Adding these just we can remove during snippet generation.
    register("accessor_list_with_braces", () -> new Ast.PropertyAccessorListOptional());
    register("accessor_list", () -> new Ast.PropertyAccessorList());

    // Wrapping to prevent Statement from disappearing
    register("assignment_expression", () -> new DefaultAstParent());
    register("do_statement", () -> new DefaultAstParent());
    register("lock_statement", () -> new DefaultAstParent());
  }
}
