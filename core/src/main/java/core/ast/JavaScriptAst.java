package core.ast;

import core.ast.api.AstIndentAligner;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.DefaultAstParent;
import core.formattedtext.FormattedTextOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavaScriptAst {

  public static class Import extends Ast.Import {

    public static String build(
      String identifier,
      boolean isDefault,
      Optional<String> from,
      Optional<String> alias
    ) {
      if (identifier.equals("star")) {
        return (
          "import *" +
          alias.map(e -> " as " + e).orElse("_") +
          from.map(e -> " from \"" + e + "<%cursor%>\"").orElse("_<%cursor%>") +
          "<%terminator%>"
        );
      }

      return (
        "import " +
        (isDefault ? "" : "{ ") +
        identifier +
        (isDefault ? "" : " }") +
        alias.map(e -> " as " + e).orElse("") +
        from.map(e -> " from \"" + e + "<%cursor%>\"").orElse("<%cursor%>") +
        "<%terminator%>"
      );
    }
  }

  public static class ReturnTypeOptional extends Ast.ReturnTypeOptional {

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
      List<Class<? extends AstParent>> types = new ArrayList<>(super.innerElementTypes());
      types.addAll(
        Arrays.asList(
          Ast.Class_.class,
          Ast.Enum.class,
          Ast.Function.class,
          Ast.Import.class,
          Ast.Interface.class,
          Ast.MarkupElement.class,
          Ast.MarkupTag.class
        )
      );
      return types;
    }

    @Override
    protected int innerMinimumBlankLines(AstNode innerElement) {
      if (innerElement instanceof Ast.Class_ || innerElement instanceof Ast.Function) {
        return 1;
      }

      return 0;
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
