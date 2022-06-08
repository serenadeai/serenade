package core.converter;

import core.ast.Ast;
import core.ast.DartAst;
import core.ast.api.AstToken;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DartParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public DartParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_DART;
  }

  @Override
  protected void registerConverters() {
    register("class_definition", () -> new Ast.Class_());
    register("constructor_signature", () -> new Ast.Constructor());
    register(
      "for_clause",
      () -> new Ast.ForClause(),
      fixer -> {
        if (fixer.hasChild(matches(Ast.BlockCollection.class))) {
          fixer.setType(() -> new Ast.ForEachClause());
        }
      }
    );
    register(
      "formal_parameter_list",
      () -> new Inlined(),
      fixer ->
        fixer.wrapSublists(
          node -> !(node.isToken("(") || node.isToken(")")),
          () -> new DartAst.Parameters()
        )
    );
    register(
      "function",
      () -> new Ast.Function(),
      fixer ->
        fixer
          .setType(
            parent -> parent.children().stream().anyMatch(child -> child instanceof Ast.Lambda),
            () -> new Ast.Lambda()
          )
          .inline(matches(Ast.Lambda.class))
    );
    register("function_arrow_variant", () -> new Ast.Lambda());
    register("getter_signature", () -> new DartAst.Getter());
    register(
      "method",
      () -> new Ast.Method(),
      fixer ->
        fixer
          .setType(
            parent -> parent.children().stream().anyMatch(child -> child instanceof DartAst.Getter),
            () -> new DartAst.Getter()
          )
          .setType(
            parent -> parent.children().stream().anyMatch(child -> child instanceof DartAst.Setter),
            () -> new DartAst.Setter()
          )
          .setType(
            parent ->
              parent.children().stream().anyMatch(child -> child instanceof Ast.Constructor),
            () -> new Ast.Constructor()
          )
          .inline(matches(DartAst.Getter.class))
          .inline(matches(DartAst.Setter.class))
          .inline(matches(Ast.Constructor.class))
    );
    register("mixin", () -> new DartAst.Mixin());
    register("mixin_list", () -> new DartAst.MixinList());
    register("mixin_list_optional", () -> new DartAst.MixinListOptional());
    register("mixin_type", () -> new DartAst.MixinType());
    register(
      "named_parameter",
      () -> new DartAst.NamedParameter(),
      fixer -> fixer.inline(matches(Ast.Parameter.class))
    );
    register("named_parameter_list", () -> new DartAst.NamedParameterList());
    register("named_parameter_list_optional", () -> new DartAst.NamedParameterListOptional());
    register("on_optional", () -> new DartAst.OnOptional());
    register("on_type", () -> new DartAst.OnType());
    register(
      "parameter",
      () -> new Ast.Parameter(),
      fixer -> fixer.inline(matches(Ast.AssignmentVariable.class))
    );
    register("positional_parameter", () -> new DartAst.PositionalParameter());
    register("positional_parameter_list", () -> new DartAst.PositionalParameterList());
    register(
      "positional_parameter_list_optional",
      () -> new DartAst.PositionalParameterListOptional()
    );
    // For parse error handling -- see comment in base converter.
    register(
      "program",
      () -> new Ast.Program(),
      fixer -> {
        if (!fixer.hasChild(child -> child instanceof DartAst.TypeDeclarationList)) {
          fixer.wrap(() -> new Ast.StatementList());
        }
      }
    );
    register("setter_signature", () -> new DartAst.Setter());
    register("type_declaration_list", () -> new DartAst.TypeDeclarationList());
  }

  @Override
  protected boolean bracesOnSameLine() {
    return true;
  }
}
