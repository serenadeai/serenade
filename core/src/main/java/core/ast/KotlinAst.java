package core.ast;

import core.ast.api.AstList;
import core.ast.api.AstListOptional;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.DefaultAstParent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class KotlinAst {

  public static class ClassMemberList extends Ast.ClassMemberList {

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (innerElement instanceof Ast.Method || innerElement instanceof Ast.Function) {
        return 1;
      }

      return 0;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(Ast.Function.class, Ast.Method.class, Ast.Property.class);
    }
  }

  public static class ImplementsListOptional extends Ast.ImplementsListOptional {

    @Override
    protected String prefix() {
      return ": ";
    }
  }

  public static class InterfaceMemberList extends Ast.InterfaceMemberList {

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (innerElement instanceof Ast.Function || innerElement instanceof Ast.Method) {
        return 1;
      }

      return 0;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(Ast.Function.class, Ast.Property.class, Ast.Method.class);
    }
  }

  public static class TypeDeclarationList extends Ast.TypeDeclarationList {

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      List<Class<? extends AstParent>> types = new ArrayList<>(super.innerElementTypes());
      types.addAll(Arrays.asList(Ast.Function.class));
      return types;
    }
  }

  public static class TypeOptional extends Ast.TypeOptional {

    @Override
    protected String prefix() {
      return ": ";
    }

    @Override
    protected int setupSpacingBeforeBecomingVisible() {
      return tokenRangeWithCommentsAndWhitespace().start;
    }

    @Override
    protected void removeSpacingBeforeBecomingInvisible() {}
  }
}
