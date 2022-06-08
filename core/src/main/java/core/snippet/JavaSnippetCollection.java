package core.snippet;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.JavaAst;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JavaSnippetCollection extends SnippetCollection {

  @Inject
  public JavaSnippetCollection(Snippets.Factory snippetsFactory) {
    initialize(snippetsFactory);
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_JAVA;
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
    return defaultClassMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> commentSnippet(SnippetTrigger trigger) {
    return defaultCommentSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> constructorSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Constructor.class,
        (parent, matches) -> {
          List<Ast.Class_> classInstanceAncestors = parent
            .ancestors(Ast.Class_.class)
            .collect(Collectors.toList());

          if (!classInstanceAncestors.isEmpty()) {
            return "public " + classInstanceAncestors.get(0).nameString() + "<%cursor%>() {\n}";
          }

          return "";
        }
      )
    );
  }

  @Override
  protected Optional<Snippet> decoratorSnippet(SnippetTrigger trigger) {
    return defaultDecoratorMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> elementSnippet(SnippetTrigger trigger) {
    return Optional.empty();
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
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> enumSnippet(SnippetTrigger trigger) {
    return defaultEnumMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> extendsSnippet(SnippetTrigger trigger) {
    return defaultExtendsMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> finallySnippet(SnippetTrigger trigger) {
    return defaultFinallyMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> forSnippet(SnippetTrigger trigger) {
    return defaultForMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> forEachSnippet(SnippetTrigger trigger) {
    return defaultForEachMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> functionSnippet(SnippetTrigger trigger) {
    return defaultMethodMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> functionInlineSnippet(SnippetTrigger trigger) {
    return methodSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> ifSnippet(SnippetTrigger trigger) {
    return defaultIfMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> implementsSnippet(SnippetTrigger trigger) {
    return defaultImplementsMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> importSnippet(SnippetTrigger trigger) {
    return defaultImportMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> interfaceSnippet(SnippetTrigger trigger) {
    return defaultInterfaceMlSnippet(trigger);
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
      snippets.addToList(
        trigger,
        Ast.Statement.class,
        (parent, matches) -> "System.out.println(<%expression%><%cursor%>)<%terminator%>",
        Map.of("expression", Arrays.asList(FormattedTextOptions.newCamelCaseExpression()))
      )
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
        new SnippetTrigger("synchronized( <%expression%>)?"),
        Ast.Statement.class,
        (parent, matches) ->
          "synchronized (" +
          (matches.get("expression") != null ? "<%expression%>" : "this") +
          ") {\n}"
      ),
      snippets.addToList(
        new SnippetTrigger("main method", "main method"),
        Ast.Method.class,
        (parent, matches) -> "public static void main<%cursor%>(String[] args) {\n}"
      ),
      snippets.addToList(
        new SnippetTrigger("modifiers <%expression%>", "modifiers"),
        Ast.Modifier.class,
        (parent, matches) -> "<%expression%>"
      )
    );
  }
}
