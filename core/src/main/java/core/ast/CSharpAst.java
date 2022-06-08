package core.ast;

import core.ast.api.AstIndentAligner;
import core.ast.api.AstList;
import core.ast.api.AstListOptional;
import core.ast.api.AstListWithLinePadding;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.DefaultAstParent;
import core.formattedtext.FormattedTextOptions;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CSharpAst {

  public static class CatchParameterOptional extends Ast.CatchParameterOptional {

    @Override
    protected String prefix() {
      return "(";
    }

    @Override
    protected String postfix() {
      return ")";
    }
  }

  public static class DeclarationMemberList extends Ast.MemberList {

    @Override
    public String containerType() {
      return "DeclarationMemberList";
    }
  }

  public static class DirectivesList extends AstListWithLinePadding<AstParent> {

    @Override
    public String containerType() {
      return "DirectivesList";
    }

    @Override
    public Class<? extends AstParent> elementType() {
      return Ast.Statement.class;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(Ast.Using.class);
    }
  }

  public static class ExtendsListOptional extends Ast.ExtendsListOptional {

    @Override
    protected String prefix() {
      return ": ";
    }
  }

  public static class InitializerExpressionList
    extends AstListWithLinePadding<Ast.InitializerExpression> {

    @Override
    public String containerType() {
      return "InitializerExpressionList";
    }

    @Override
    public String delimiter() {
      return ",";
    }

    @Override
    public Class<? extends Ast.InitializerExpression> elementType() {
      return Ast.InitializerExpression.class;
    }
  }

  public static class TypeParameterConstraintListOptional
    extends Ast.TypeParameterConstraintListOptional {

    @Override
    protected String prefix() {
      return "";
    }
  }

  public static class UsingBlock extends DefaultAstParent {}
}
