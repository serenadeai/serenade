package core.converter;

import core.ast.Ast;
import core.ast.RustAst;
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
public class RustParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public RustParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_RUST;
  }

  @Override
  protected void registerConverters() {
    register("statement_list", () -> new RustAst.StatementList());
    register("implements_list_optional", () -> new RustAst.ImplementsListOptional());
    register("type_optional", () -> new RustAst.TypeOptional());
    register("return_type_optional", () -> new RustAst.ReturnTypeOptional());
    register("struct_member_list", () -> new RustAst.StructMemberList());
    register(
      "return_value",
      () -> new Ast.ReturnValue(),
      fixer ->
        fixer.setType(
          parent -> parent.children().stream().anyMatch(child -> child instanceof Ast.EnclosedBody),
          () -> new Inlined()
        )
    );
    register("second_modifier_list", () -> new Ast.ModifierList());

    register(
      "type",
      () -> new Ast.Type(),
      fixer -> fixer.inline(matches(Ast.Identifier.class)).inline(matches(Ast.Type.class))
    );
    register("trait_bounds", () -> new Ast.TypeParameterConstraintList());
    register("trait_bound", () -> new Ast.TypeParameterConstraintType());

    // These are necessary because statement/return_value inline themselves when their children is
    // an EnclosedBody. However we don't want this behavior in certain cases.
    register("match_expression", () -> new DefaultAstParent());
    register("struct_expression", () -> new DefaultAstParent());
    register("unsafe_block", () -> new DefaultAstParent());
    register("async_block", () -> new DefaultAstParent());
    register("const_block", () -> new DefaultAstParent());
  }

  @Override
  protected boolean bracesOnSameLine() {
    return true;
  }
}
