package core.ast;

import core.ast.api.AstListOptional;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RustAst {

  public static class ImplementsListOptional extends Ast.ImplementsListOptional {

    @Override
    protected String prefix() {
      return "";
    }

    @Override
    protected String postfix() {
      return " for";
    }
  }

  public static class ReturnTypeOptional extends Ast.ReturnTypeOptional {

    @Override
    protected String prefix() {
      return "->";
    }
  }

  public static class StructMemberList extends Ast.StructMemberList {

    @Override
    public String delimiter() {
      return ",";
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

  public static class StatementList extends Ast.StatementList {

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      List<Class<? extends AstParent>> inner = new ArrayList<>(super.innerElementTypes());
      inner.add(Ast.Import.class);
      return inner;
    }
  }
}
