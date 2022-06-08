package core.ast;

import core.ast.api.AstIndentAligner;
import core.ast.api.AstList;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.util.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ScssAst {

  public static class Import extends Ast.Import {

    public List<AstNode> importName() {
      return children().subList(1, children.size() - 1);
    }
  }

  public static class StatementList extends Ast.StatementList {

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      List<Class<? extends AstParent>> types = new ArrayList<>(super.innerElementTypes());

      types.addAll(
        Arrays.asList(
          Ast.CssMixin.class,
          Ast.CssInclude.class,
          Ast.CssMedia.class,
          Ast.CssRuleset.class,
          Ast.KeyValuePair.class
        )
      );
      return types;
    }
  }
}
