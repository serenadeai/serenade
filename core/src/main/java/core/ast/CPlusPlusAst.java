package core.ast;

import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.DefaultAstParent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CPlusPlusAst {

  public static class ClassMemberList extends Ast.ClassMemberList {

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (
        innerElement instanceof Ast.Function ||
        innerElement instanceof Ast.Class_ ||
        innerElement instanceof Ast.Method
      ) {
        return 1;
      }

      return 0;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(
        Ast.Class_.class,
        Ast.Enum.class,
        Ast.Function.class,
        Ast.Method.class,
        Ast.Modifier.class,
        Ast.Property.class
      );
    }
  }

  public static class ExtendsOptional extends Ast.ExtendsOptional {

    @Override
    protected int setupSpacingBeforeBecomingVisible() {
      return tokenRangeWithCommentsAndWhitespace().start;
    }

    @Override
    protected void removeSpacingBeforeBecomingInvisible() {}

    @Override
    protected String prefix() {
      return ": ";
    }
  }

  public static class ExtendsListOptional extends Ast.ExtendsListOptional {

    @Override
    protected int setupSpacingBeforeBecomingVisible() {
      return tokenRangeWithCommentsAndWhitespace().start;
    }

    @Override
    protected void removeSpacingBeforeBecomingInvisible() {}

    @Override
    protected String prefix() {
      return ": ";
    }
  }

  public static class Include extends Ast.Import {}

  public static class ReturnTypeOptional extends Ast.ReturnTypeOptional {

    @Override
    protected String prefix() {
      return "-> ";
    }
  }

  public static class StatementList extends Ast.StatementList {

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (
        indent().equals("") &&
        (
          innerElement instanceof Ast.Class_ ||
          innerElement instanceof Ast.Function ||
          innerElement instanceof Ast.Struct
        )
      ) {
        return 1;
      }

      return 0;
    }
  }

  public static class TemporaryFunctionDeclarator extends DefaultAstParent {}
}
