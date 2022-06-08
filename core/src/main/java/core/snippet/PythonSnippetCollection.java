package core.snippet;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.PythonAst;
import core.ast.api.AstList;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import core.util.Diff;
import core.util.TextStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PythonSnippetCollection extends SnippetCollection {

  @Inject
  public PythonSnippetCollection(Snippets.Factory snippetsFactory) {
    initialize(snippetsFactory);
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_PYTHON;
  }

  @Override
  protected Optional<Snippet> bareSnippet(SnippetTrigger trigger) {
    return defaultBareMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> argumentSnippet(SnippetTrigger trigger) {
    return defaultArgumentMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> attributeSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> catchSnippet(SnippetTrigger trigger) {
    return defaultCatchMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> classSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToTopLevelStatementListWithMlSnippet(trigger, PythonAst.Class_.class)
    );
  }

  @Override
  protected Optional<Snippet> commentSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addComment(trigger, Ast.Comment.class, (parent, matches) -> "# <%text%>")
    );
  }

  @Override
  protected Optional<Snippet> constructorSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Constructor.class,
        (parent, matches) -> "def __init__<%cursor%>(self):\n<%indent%>pass"
      )
    );
  }

  @Override
  protected Optional<Snippet> decoratorSnippet(SnippetTrigger trigger) {
    return defaultDecoratorMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> elementSnippet(SnippetTrigger trigger) {
    return defaultElementMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> elseSnippet(SnippetTrigger trigger) {
    return defaultElseMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> elseIfSnippet(SnippetTrigger trigger) {
    return defaultElseIfMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> emptyTagSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> entrySnippet(SnippetTrigger trigger) {
    return defaultEntryMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> enumSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToTopLevelStatementList(
        trigger,
        Ast.Enum.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            "class <%identifier%><%cursor%>(enum.Enum):\n<%indent%>pass"
          )
      )
    );
  }

  @Override
  protected Optional<Snippet> extendsSnippet(SnippetTrigger trigger) {
    return defaultExtendsToListMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> finallySnippet(SnippetTrigger trigger) {
    return defaultFinallyMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> forSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> forEachSnippet(SnippetTrigger trigger) {
    return defaultForEachMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> functionSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToTopLevelStatementListWithMlSnippet(trigger, PythonAst.Function.class)
    );
  }

  @Override
  protected Optional<Snippet> functionInlineSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        PythonAst.Function.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            moveOptionalTypeToParensArrowPostfix(matches, "def <%identifier%>:\n<%indent%>pass")
          )
      )
    );
  }

  @Override
  protected Optional<Snippet> ifSnippet(SnippetTrigger trigger) {
    return defaultIfMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> implementsSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> importSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, PythonAst.Import.class));
  }

  @Override
  protected Optional<Snippet> interfaceSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> methodSnippet(SnippetTrigger trigger) {
    return defaultMethodMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> modifierSnippet(SnippetTrigger trigger) {
    return defaultModifierMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> parameterSnippet(SnippetTrigger trigger) {
    return defaultParameterMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> printSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(trigger, Ast.Statement.class, (parent, matches) -> "print(<%expression%>)")
    );
  }

  @Override
  protected Optional<Snippet> propertySnippet(SnippetTrigger trigger) {
    return defaultPropertyMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> returnTypeSnippet(SnippetTrigger trigger) {
    return defaultReturnTypeMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> returnValueSnippet(SnippetTrigger trigger) {
    return defaultReturnValueMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> tagSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> trySnippet(SnippetTrigger trigger) {
    return defaultTryMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> typeSnippet(SnippetTrigger trigger) {
    return defaultTypeMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> whileSnippet(SnippetTrigger trigger) {
    return defaultWhileMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> withSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected List<Snippet> extraSnippets() {
    return Arrays.asList(
      snippets.addToList(
        new SnippetTrigger("(if name equals )?main", "main"),
        Ast.If.class,
        (parent, matches) -> "if __name__ == \"__main__\"<%cursor%>:\n<%indent%>pass"
      )
    );
  }
}
