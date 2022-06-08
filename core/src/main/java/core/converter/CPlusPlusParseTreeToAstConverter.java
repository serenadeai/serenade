package core.converter;

import core.ast.Ast;
import core.ast.CPlusPlusAst;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CPlusPlusParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public CPlusPlusParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_CPLUSPLUS;
  }

  @Override
  protected void registerConverters() {
    register("preproc_include", () -> new CPlusPlusAst.Include());
    register("using_declaration", () -> new Ast.Using());
    register(
      "program",
      () -> new Ast.Program(),
      fixer -> {
        if (!fixer.hasChild(matches(CPlusPlusAst.StatementList.class))) {
          fixer.wrap(() -> new CPlusPlusAst.StatementList());
        }
      }
    );
    register("statement_list", () -> new CPlusPlusAst.StatementList());
    register("class_member_list", () -> new CPlusPlusAst.ClassMemberList());
    register("extends_list_optional", () -> new CPlusPlusAst.ExtendsListOptional());
    register("extends_optional", () -> new CPlusPlusAst.ExtendsOptional());

    register("second_modifier_list", () -> new Ast.ModifierList());

    register("class_specifier", () -> new Ast.Class_());
    register(
      "struct_specifier",
      () -> new Ast.Struct(),
      fixer ->
        fixer.fixChildren(
          childFixer ->
            childFixer.changeChildType(
              () -> new CPlusPlusAst.ClassMemberList(),
              () -> new Ast.StructMemberList()
            )
        )
    );

    register(
      "identifier",
      () -> new Ast.Identifier(),
      fixer ->
        fixer
          .inline(matches(Ast.Identifier.class))
          .inline(matches(Ast.ModifierList.class))
          .inline(matches(Ast.Modifier.class))
    );
    register(
      "lambda",
      () -> new Ast.Lambda(),
      fixer -> fixer.inline(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class))
    );
    register(
      "function_definition",
      () -> new Ast.Function(),
      fixer -> fixer.inline(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class))
    );
    register(
      Arrays.asList("inline_method_definition"),
      () -> new Ast.Method(),
      fixer -> fixer.inline(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class))
    );
    register(
      Arrays.asList("constructor_or_destructor_definition"),
      () -> new Ast.Constructor(),
      fixer -> fixer.inline(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class))
    );

    register("delete_method_clause", () -> new Ast.DeleteMethodClause());
    register("default_method_clause", () -> new Ast.DefaultMethodClause());
    register("lambda_capture_specifier", () -> new Ast.LambdaCaptureSpecifier());
    register("template_argument_list", () -> new Ast.TemplateArgumentList());
    register(
      "attribute",
      () -> new Ast.AttributeList(),
      fixer -> fixer.wrap(() -> new Ast.Attribute())
    );

    // String special-case. Can rename, but need to rename both...
    register(Arrays.asList("string_literal", "raw_string_literal"), this::convertString);

    // TODO: Better solution may be to make a copy of the parameter definition that only happens
    // within the catch.
    register(
      "catch_parameter",
      () -> new Ast.CatchParameter(),
      fixer -> fixer.inline(matches(Ast.Parameter.class))
    );

    register(
      Arrays.asList(
        "function_declarator",
        "function_field_declarator",
        "function_type_declarator",
        "abstract_function_declarator"
      ),
      () -> new CPlusPlusAst.TemporaryFunctionDeclarator()
    );

    register(
      "top_level_declarator",
      () -> new Ast.Assignment(),
      fixer -> {
        if (!fixer.hasChild(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class))) {
          // If it's a regular assignment, use the standard assignment fixer.
          fixer
            .wrapSublists(() -> new Ast.AssignmentVariableList())
            .splitLeadingEqualsFromChild(matches(Ast.AssignmentValue.class))
            .wrapSublists(() -> new Ast.AssignmentValueList())
            .splitLeadingEqualsFromChild(matches(Ast.AssignmentValueList.class))
            .wrapIndividually(
              matches(Ast.AssignmentValueList.class),
              () -> new Ast.AssignmentValueListOptional()
            );
        } else {
          fixer.setType(() -> new Inlined());
        }
      }
    );

    register(
      "maybe_assignment_variable",
      () -> new Ast.AssignmentVariable(),
      fixer -> {
        if (fixer.hasChild(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class))) {
          fixer.setType(() -> new Inlined());
        }
      }
    );

    register(
      "maybe_assignment_variable_list",
      () -> new Ast.AssignmentVariableList(),
      fixer -> {
        if (fixer.hasChild(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class))) {
          fixer.setType(() -> new Inlined());
        }
      }
    );

    register(
      "field_declaration_maybe_assignment",
      () -> new Ast.AssignmentList(),
      fixer -> {
        if (fixer.hasChild(matches(Ast.AssignmentVariableList.class))) {
          fixer.wrap(() -> new Ast.Assignment());
        } else {
          fixer.setType(() -> new Inlined());
        }
      }
    );

    // Only used in member lists.
    register(
      "field_declaration",
      () -> new Ast.Property(),
      fixer -> {
        if (fixer.hasChild(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class))) {
          fixer
            .setType(() -> new Ast.Function())
            .inline(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class));
          if (!fixer.hasChild(matches(Ast.EnclosedBody.class))) {
            fixer.setType(() -> new Ast.Prototype());
          }
        }
      }
    );

    // TODO: We want something like this.
    register(
      "declaration",
      () -> new Ast.VariableDeclaration(),
      fixer -> {
        if (!fixer.hasChild(matches(Ast.Assignment.class))) {
          fixer
            .setType(() -> new Ast.Function())
            .inline(matches(CPlusPlusAst.TemporaryFunctionDeclarator.class));
          if (!fixer.hasChild(matches(Ast.EnclosedBody.class))) {
            fixer.setType(() -> new Ast.Prototype());
          }
        }
      }
    );

    // Asserts seem to be just calls with a name?
    register(
      "call",
      () -> new Ast.Call(),
      fixer -> {
        if (
          fixer.parent
            .children()
            .stream()
            .anyMatch(
              child ->
                child.getClass().equals(Ast.Identifier.class) &&
                child.children().stream().anyMatch(token -> token.isToken("assert"))
            )
        ) {
          fixer.setType(() -> new Ast.Assert());
        }
      }
    );

    // Due to how the C++ specification defines declarations
    // https://en.cppreference.com/w/cpp/language/declarations#Specifiers
    // the tree-sitter grammar is written to align with that, and thus conflicts with our notion of
    // Ast.Type, if we want to have the */& as part of the Type.
    // We don't change how the grammar is written for this, but we write a wrapper field for
    // all the contents, tag the type, and pull out the identifier and other potential symbols.
    register(
      "type",
      () -> new Ast.TypeOptional(),
      fixer ->
        fixer
          .inline(matches(Ast.TypeOptional.class))
          .inline(matches(Ast.Type.class))
          .inline(matches(Ast.Identifier.class))
          .inline(matches(Ast.Enum.class))
          .inline(matches(Ast.Struct.class))
          .inline(matches(Ast.Class_.class))
          .wrap(() -> new Ast.Type())
    );

    register("return_type", () -> new Ast.Type());
  }

  @Override
  protected boolean bracesOnSameLine() {
    return true;
  }
}
