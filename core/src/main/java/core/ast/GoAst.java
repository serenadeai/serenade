package core.ast;

import core.ast.api.AstIndentAligner;
import core.ast.api.AstList;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.DefaultAstParent;
import core.formattedtext.FormattedTextOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GoAst {

  public static class ConstDeclaration extends AstIndentAligner {}

  public static class ConstSpec extends DefaultAstParent {}

  public static class ConstSpecList extends AstList<ConstSpec> {

    @Override
    public boolean isMultiline() {
      return true;
    }

    @Override
    public String containerType() {
      return "ConstSpecList";
    }

    @Override
    public Class<? extends ConstSpec> elementType() {
      return ConstSpec.class;
    }
  }

  public static class ImportSpecifierList extends Ast.ImportSpecifierList {

    @Override
    public String delimiter() {
      return "";
    }

    @Override
    public boolean isMultiline() {
      return true;
    }
  }

  public static class InterfaceMemberList extends Ast.InterfaceMemberList {

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      return Arrays.asList(Ast.Method.class);
    }
  }

  public static class StatementList extends Ast.StatementList {

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      List<Class<? extends AstParent>> types = new ArrayList<>(super.innerElementTypes());
      types.addAll(Arrays.asList(Ast.Method.class));
      return types;
    }

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (innerElement instanceof Ast.Function || innerElement instanceof Ast.Method) {
        return 1;
      }

      return 0;
    }
  }
}
