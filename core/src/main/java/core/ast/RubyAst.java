package core.ast;

import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.DefaultAstParent;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RubyAst {

  public static class ExtendsOptional extends Ast.ExtendsOptional {

    @Override
    protected String prefix() {
      return "< ";
    }
  }

  public static class StatementList extends Ast.StatementList {

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (innerElement instanceof Ast.Class_) {
        return 1;
      }

      return 0;
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      List<Class<? extends AstParent>> inner = new ArrayList<>(super.innerElementTypes());
      inner.add(Ast.Import.class);
      return inner;
    }
  }

  public static class ParameterListOptional extends Ast.ParameterListOptional {

    @Override
    protected int setupSpacingBeforeBecomingVisible() {
      // Don't introduce a space when we become visible.
      return tokenRangeWithCommentsAndWhitespace().start;
    }
  }
}
