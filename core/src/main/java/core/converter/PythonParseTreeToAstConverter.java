package core.converter;

import core.ast.Ast;
import core.ast.PythonAst;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.converter.ParseTreeToAstConverter;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PythonParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public PythonParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_PYTHON;
  }

  @Override
  protected void registerConverters() {
    // Python-specific AST stuff.
    register("indentation_offset_body", () -> new PythonAst.EnclosedBody());

    // Can't change the grammar easily to handle these, so do manual fixing that we generally want to avoid.
    register(
      "indentation_offset_body_placeholder",
      () -> new PythonAst.EnclosedBody(),
      fixer -> fixer.parent.children().add(new PythonAst.StatementList())
    );
    register(
      "indentation_offset_body_class_placeholder",
      () -> new PythonAst.EnclosedBody(),
      fixer -> fixer.parent.children().add(new PythonAst.ClassMemberList())
    );

    register("return_type_optional", () -> new PythonAst.ReturnTypeOptional());
    register(
      "program",
      () -> new Ast.Program(),
      fixer -> {
        if (!fixer.hasChild(matches(PythonAst.StatementList.class))) {
          fixer.wrap(() -> new PythonAst.StatementList());
        }
      }
    );
    register("statement_list", () -> new PythonAst.StatementList());
    register("type_optional", () -> new PythonAst.TypeOptional());
    register("extends_list_optional", () -> new PythonAst.ExtendsListOptional());
    register("class_definition", () -> new PythonAst.Class_());
    register("parameter_list", () -> new PythonAst.ParameterList());
    register("class_member_list", () -> new PythonAst.ClassMemberList());

    // Concepts we can merge across languages
    register("string_text", () -> new Ast.StringText());
    register("import_statement", () -> new PythonAst.Import());

    // We can't fix functions without doing it recursively from classes, due to grammar constraints.
    register("function_definition", () -> new Ast.Function());
    register(
      "member",
      () -> new Ast.Member(),
      fixer ->
        fixer
          .changeChildType(
            child ->
              child instanceof Ast.Function &&
              "__init__".equals(
                  child
                    .name()
                    .map(
                      id ->
                        id.children().stream().map(t -> t.code()).collect(Collectors.joining(""))
                    )
                    .orElse("")
                ),
            () -> new Ast.Constructor()
          )
          .changeChildType(() -> new Ast.Function(), () -> new Ast.Method())
          .changeChildType(() -> new Ast.VariableDeclaration(), () -> new Ast.Property())
    );
  }
}
