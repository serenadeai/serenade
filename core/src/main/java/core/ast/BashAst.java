package core.ast;

import core.ast.Ast;
import java.util.Optional;

public class BashAst {

  public static class ArgumentList extends Ast.ArgumentList {

    @Override
    public String delimiter() {
      return "";
    }
  }
}
