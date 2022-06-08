package core.ast;

import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.DefaultAstParent;
import core.ast.api.IndentationUtils;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PythonAst {

  private static IndentationUtils indentationUtils = new IndentationUtils();
  private static Whitespace whitespace = new Whitespace();

  public static class EnclosedBody extends Ast.EnclosedBody {

    private Whitespace whitespace = new Whitespace();

    protected String defaultIndentationToken() {
      return whitespace.indentationToken(tree.root.codeWithCommentsAndWhitespace(), 4);
    }

    @Override
    public AstNode inner() {
      // there are no braces so the offset is different.
      return children().get(0);
    }
  }

  public static class Class_ extends Ast.Class_ {

    @Override
    public int setupSpacingForChildBecomingVisible(AstNode child) {
      // neither the decorator list nor the parens around the parent list need spacing setup.
      return child.tokenRangeWithCommentsAndWhitespace().stop;
    }

    @Override
    public void removeSpacingForChildBecomingInvisible(AstNode child) {
      // see setupSpacingForChildBecomingVisible
    }
  }

  public static class ClassMemberList extends Ast.ClassMemberList {

    @Override
    public boolean boundedBelow() {
      return indent().equals("");
    }

    protected Optional<AstParent> placeholder() {
      return Optional.of(tree().factory.create(new Ast.Member(), "pass"));
    }

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (
        innerElement instanceof Ast.Method ||
        innerElement instanceof Ast.Function ||
        innerElement instanceof Ast.TypeDeclaration
      ) {
        return 1;
      }

      return 0;
    }
  }

  public static class Enum extends Class_ {}

  public static class ExtendsListOptional extends Ast.ExtendsListOptional {

    @Override
    protected String prefix() {
      return "(";
    }

    @Override
    protected String postfix() {
      return ")";
    }
  }

  public static class Function extends Ast.Function {

    @Override
    public int setupSpacingForChildBecomingVisible(AstNode child) {
      // neither the decorator list nor the parens around the parent list need spacing setup.
      return child.tokenRangeWithCommentsAndWhitespace().stop;
    }

    @Override
    public void removeSpacingForChildBecomingInvisible(AstNode child) {
      // see setupSpacingForChildBecomingVisible
    }
  }

  public static class Import extends Ast.Import {}

  public static class Parameter extends Ast.Parameter {

    @Override
    public Optional<Ast.Splat> splat() {
      return child(Ast.Splat.class);
    }
  }

  public static class ParameterList extends Ast.ParameterList {

    private final List<String> possiblePrefixes = Arrays.asList("cls", "self");

    public void clear() {
      Optional<AstParent> prefix = elements()
        .stream()
        .findFirst()
        .filter(c -> possiblePrefixes.contains(c.code()))
        .map(c -> tree().factory.clone(c));
      super.clear();
      prefix.ifPresent(p -> add(0, p));
    }
  }

  public static class Property extends Ast.Property {

    public static String build(
      String identifier,
      Optional<String> type,
      Optional<String> expression
    ) {
      return (
        identifier + type.map(e -> ": " + e).orElse("") + expression.map(e -> " = " + e).orElse("")
      );
    }
  }

  public static class StatementList extends Ast.StatementList {

    @Override
    public boolean boundedBelow() {
      return indent().equals("");
    }

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (
        indent().equals("") &&
        (innerElement instanceof Ast.Class_ || innerElement instanceof Ast.Function)
      ) {
        return 2;
      } else if (innerElement instanceof Ast.Method) {
        return 1;
      }
      return 0;
    }

    protected Optional<AstParent> placeholder() {
      if (indent().equals("")) {
        return Optional.empty();
      }
      return Optional.of(tree().factory.create(new Ast.Statement(), "pass"));
    }

    @Override
    public List<Class<? extends AstParent>> innerElementTypes() {
      List<Class<? extends AstParent>> inner = new ArrayList<>(super.innerElementTypes());
      inner.add(Ast.Import.class);
      return inner;
    }
  }

  public static class ReturnTypeOptional extends Ast.ReturnTypeOptional {

    @Override
    protected String prefix() {
      return "-> ";
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
