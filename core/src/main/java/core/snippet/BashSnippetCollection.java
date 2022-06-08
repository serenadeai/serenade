package core.snippet;

import core.ast.Ast;
import core.ast.BashAst;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BashSnippetCollection extends SnippetCollection {

  @Inject
  public BashSnippetCollection(Snippets.Factory snippetsFactory) {
    initialize(snippetsFactory);
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_BASH;
  }

  @Override
  protected Optional<Snippet> bareSnippet(SnippetTrigger trigger) {
    return defaultBareSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> argumentSnippet(SnippetTrigger trigger) {
    return defaultArgumentSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> attributeSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> catchSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> classSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> commentSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addComment(trigger, Ast.Comment.class, (parent, matches) -> "# <%text%>")
    );
  }

  @Override
  protected Optional<Snippet> constructorSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> decoratorSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> elementSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> elseSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToOptional(
        trigger,
        Ast.ElseClause.class,
        (parent, matches) ->
          "else<%cursor%>" +
          optionalSlot(matches, "expression").map(e -> "\n<%indent%>" + e).orElse("")
      )
    );
  }

  @Override
  protected Optional<Snippet> elseIfSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.ElseIfClause.class,
        (parent, matches) ->
          "elif [[ " + optionalSlot(matches, "condition").orElse("1") + "<%cursor%> ]]; then"
      )
    );
  }

  @Override
  protected Optional<Snippet> emptyTagSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> entrySnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> enumSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> extendsSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> finallySnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> forSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> forEachSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Statement.class,
        (parent, matches) -> "for <%expression%><%cursor%> ; do\ndone"
      )
    );
  }

  @Override
  protected Optional<Snippet> functionSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToTopLevelStatementList(
        trigger,
        Ast.Function.class,
        (parent, matches) -> "<%identifier%><%cursor%>() {\n}"
      )
    );
  }

  @Override
  protected Optional<Snippet> functionInlineSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Function.class,
        (parent, matches) -> "<%identifier%><%cursor%>() {\n}"
      )
    );
  }

  @Override
  protected Optional<Snippet> ifSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Statement.class,
        (parent, matches) ->
          "if [[ " + optionalSlot(matches, "condition").orElse("1") + "<%cursor%> ]]; then\nfi"
      )
    );
  }

  @Override
  protected Optional<Snippet> implementsSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> importSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> interfaceSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> methodSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> modifierSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> parameterSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> printSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Statement.class,
        (parent, matches) -> "echo <%expression%><%cursor%><%terminator%>"
      )
    );
  }

  @Override
  protected Optional<Snippet> propertySnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> returnTypeSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> returnValueSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> tagSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> trySnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> typeSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> whileSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> withSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }
}
