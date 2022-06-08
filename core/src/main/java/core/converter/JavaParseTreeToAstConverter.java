package core.converter;

import core.ast.Ast;
import core.ast.Ast;
import core.ast.JavaAst;
import core.ast.JavaAst;
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
public class JavaParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public JavaParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_JAVA;
  }

  @Override
  protected void registerConverters() {
    register(
      "parameter",
      () -> new Ast.Parameter(),
      fixer -> fixer.inline(matches(Ast.AssignmentVariable.class))
    );

    register("string_literal", this::convertString);

    // Wrap these, so the enclosed body here doesn't force the statement to be removed.
    register("do_statement", () -> new DefaultAstParent());
    register("synchronized_statement", () -> new DefaultAstParent());
    register("try_with_resources_statement", () -> new DefaultAstParent());
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
