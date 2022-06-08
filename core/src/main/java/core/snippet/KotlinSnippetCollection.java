package core.snippet;

import core.ast.Ast;
import core.ast.KotlinAst;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KotlinSnippetCollection extends SnippetCollection {

  @Inject
  public KotlinSnippetCollection(Snippets.Factory snippetsFactory) {
    initialize(snippetsFactory);
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_KOTLIN;
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
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.CatchClause.class,
        (parent, matches) ->
          "catch (e" +
          optionalSlot(matches, "expression").map(e -> ": " + e).orElse("") +
          "<%cursor%>) {\n}"
      )
    );
  }

  @Override
  protected Optional<Snippet> classSnippet(SnippetTrigger trigger) {
    return defaultClassSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> commentSnippet(SnippetTrigger trigger) {
    return defaultCommentSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> constructorSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> decoratorSnippet(SnippetTrigger trigger) {
    return defaultDecoratorSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> elementSnippet(SnippetTrigger trigger) {
    return defaultElementSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> elseSnippet(SnippetTrigger trigger) {
    return defaultElseSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> elseIfSnippet(SnippetTrigger trigger) {
    return defaultElseIfSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> emptyTagSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> entrySnippet(SnippetTrigger trigger) {
    return defaultEntrySnippet(trigger);
  }

  @Override
  protected Optional<Snippet> enumSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Enum.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            "enum class <%identifier%><%cursor%> {\n}"
          )
      )
    );
  }

  @Override
  protected Optional<Snippet> extendsSnippet(SnippetTrigger trigger) {
    return implementsSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> finallySnippet(SnippetTrigger trigger) {
    return defaultFinallySnippet(trigger);
  }

  @Override
  protected Optional<Snippet> forSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(trigger, Ast.Statement.class, (parent, matches) -> "for (<%cursor%>) {\n}")
    );
  }

  @Override
  protected Optional<Snippet> forEachSnippet(SnippetTrigger trigger) {
    return defaultForEachSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> functionSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Function.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            moveOptionalTypeToParensColonPostfix(matches, "fun <%identifier%> {\n}")
          )
      )
    );
  }

  @Override
  protected Optional<Snippet> functionInlineSnippet(SnippetTrigger trigger) {
    return functionSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> ifSnippet(SnippetTrigger trigger) {
    return defaultIfSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> implementsSnippet(SnippetTrigger trigger) {
    return defaultImplementsSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> importSnippet(SnippetTrigger trigger) {
    return defaultImportSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> interfaceSnippet(SnippetTrigger trigger) {
    return defaultInterfaceSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> methodSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Method.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            moveOptionalTypeToParensColonPostfix(
              matches,
              "fun <%identifier%>" +
              (parent.ancestor(Ast.Interface.class).isEmpty() ? " {\n}" : "<%terminator%>")
            )
          )
      )
    );
  }

  @Override
  protected Optional<Snippet> modifierSnippet(SnippetTrigger trigger) {
    return defaultModifierSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> parameterSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Parameter.class,
        (parent, matches) -> moveOptionalTypeToColonPostfix(matches, "<%identifier%>")
      )
    );
  }

  @Override
  protected Optional<Snippet> printSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Statement.class,
        (parent, matches) -> "print(<%expression%><%cursor%>)"
      )
    );
  }

  @Override
  protected Optional<Snippet> propertySnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Property.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            moveOptionalTypeToColonPostfix(matches, "<%identifier%>")
          )
      )
    );
  }

  @Override
  protected Optional<Snippet> returnTypeSnippet(SnippetTrigger trigger) {
    return defaultReturnTypeSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> returnValueSnippet(SnippetTrigger trigger) {
    return defaultReturnValueSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> tagSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> trySnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Statement.class,
        (parent, matches) ->
          "try<%cursor%> {\n" +
          optionalSlot(matches, "expression").map(e -> "<%indent%>" + e + "\n").orElse("") +
          "} catch (e) {\n}"
      )
    );
  }

  @Override
  protected Optional<Snippet> typeSnippet(SnippetTrigger trigger) {
    return defaultTypeSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> whileSnippet(SnippetTrigger trigger) {
    return defaultWhileSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> withSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected List<Snippet> extraSnippets() {
    return Arrays.asList(
      snippets.addToList(
        new SnippetTrigger("assert <%expression%>"),
        Ast.Statement.class,
        (parent, matches) -> "assert(<%expression%><%cursor%>)"
      )
    );
  }
}
