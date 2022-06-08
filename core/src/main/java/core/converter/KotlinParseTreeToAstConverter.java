package core.converter;

import core.ast.Ast;
import core.ast.KotlinAst;
import core.ast.api.AstList;
import core.ast.api.AstNode;
import core.ast.api.AstOptional;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KotlinParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public KotlinParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_KOTLIN;
  }

  @Override
  protected void registerConverters() {
    register("class_member_list", () -> new KotlinAst.ClassMemberList());
    register("interface_member_list", () -> new KotlinAst.InterfaceMemberList());
    register("implements_list_optional", () -> new KotlinAst.ImplementsListOptional());
    register("type_optional", () -> new KotlinAst.TypeOptional());
    register("type_declaration_list", () -> new KotlinAst.TypeDeclarationList());

    // Use this converter instead of splitting in the grammar for now. Should not be hard to split, but
    // don't want to make drastic changes to a pretty quickly changing base grammar. This basically
    // sets the type to class vs. enum vs. interface properly, and also changes the enclosed member list type.
    register("class_body", () -> new Ast.EnclosedBody());
    register("enum_class_body", () -> new Ast.EnclosedBody());
    register(
      "class_declaration",
      () -> new Ast.Class_(),
      fixer -> {
        if (
          fixer.parent
            .children()
            .stream()
            .anyMatch(
              child -> child.getClass().equals(AstToken.class) && child.code().equals("enum")
            )
        ) {
          fixer
            .setType(() -> new Ast.Enum())
            .fixChildren(
              childFixer -> {
                if (childFixer.parent instanceof Ast.EnclosedBody) {
                  childFixer.changeChildType(
                    grandchild -> grandchild instanceof Ast.MemberList,
                    () -> new Ast.EnumMemberList()
                  );
                }
                return childFixer;
              }
            );
        } else if (
          fixer.parent
            .children()
            .stream()
            .anyMatch(
              child -> child.getClass().equals(AstToken.class) && child.code().equals("interface")
            )
        ) {
          fixer
            .setType(() -> new Ast.Interface())
            .fixChildren(
              childFixer -> {
                if (childFixer.parent instanceof Ast.EnclosedBody) {
                  childFixer.changeChildType(
                    grandchild -> grandchild instanceof Ast.MemberList,
                    () -> new KotlinAst.InterfaceMemberList()
                  );
                }
                return childFixer;
              }
            );
        } else {
          fixer.fixChildren(
            childFixer -> {
              if (childFixer.parent instanceof Ast.EnclosedBody) {
                childFixer.changeChildType(
                  grandchild -> grandchild instanceof Ast.MemberList,
                  () -> new KotlinAst.ClassMemberList()
                );
              }
              return childFixer;
            }
          );
        }
      }
    );

    register(
      "call",
      () -> new Ast.Call(),
      fixer -> {
        if (
          fixer.parent
            .children()
            .stream()
            .anyMatch(
              child -> child.getClass().equals(AstToken.class) && child.code().equals("assert")
            )
        ) {
          fixer.setType(() -> new Ast.Assert());
        }
      }
    );
  }

  @Override
  protected boolean bracesOnSameLine() {
    return true;
  }
}
