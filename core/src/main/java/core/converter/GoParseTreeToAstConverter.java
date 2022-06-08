package core.converter;

import core.ast.Ast;
import core.ast.GoAst;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import core.util.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GoParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public GoParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_GO;
  }

  @Override
  protected void registerConverters() {
    register("const_declaration", () -> new Ast.EnclosedBody());
    register("const_spec", () -> new GoAst.ConstSpec());
    register("const_spec_list", () -> new GoAst.ConstSpecList());
    register("interface_member_list", () -> new GoAst.InterfaceMemberList());
    register("statement_list", () -> new GoAst.StatementList());
    register("string", this::convertString);

    register(
      "function",
      () -> new Ast.Function(),
      fixer -> fixer.inline(matches(Ast.Function.class)).inline(matches(Ast.Method.class))
    );

    register("labeled_statement", () -> new Ast.Statement());
  }

  protected AstNode convertString(String source, ParseTree node) {
    Ast.String_ result = new Ast.String_();
    List<AstNode> children = new ArrayList<>();

    children.add(
      createToken(new AstToken(), source, new Range(node.getStart(), node.getStart() + 1))
    );
    Ast.StringText stringText = new Ast.StringText();
    stringText.setChildren(
      convertNonWhitespaceToTokens(
        () -> new AstToken(),
        source,
        new Range(node.getStart() + 1, node.getStop() - 1)
      )
    );
    children.add(stringText);
    children.add(
      createToken(new AstToken(), source, new Range(node.getStop() - 1, node.getStop()))
    );

    result.setChildren(children);
    return result;
  }

  @Override
  protected boolean bracesOnSameLine() {
    return true;
  }
}
