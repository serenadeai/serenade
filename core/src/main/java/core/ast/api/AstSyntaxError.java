package core.ast.api;

public class AstSyntaxError extends RuntimeException {

  public final int line;
  public final int column;
  public final int position;

  public AstSyntaxError(int line, int column, int position) {
    super("Syntax error on line " + (line + 1) + ", column " + (column + 1));
    this.line = line;
    this.column = column;
    this.position = position;
  }
}
