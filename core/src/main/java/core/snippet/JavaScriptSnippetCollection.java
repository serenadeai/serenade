package core.snippet;

import core.ast.Ast;
import core.ast.JavaScriptAst;
import core.ast.api.AstList;
import core.ast.api.AstListWithLinePadding;
import core.closeness.ClosestObjectFinder;
import core.exception.CannotFindInsertionPoint;
import core.exception.ObjectNotFound;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import core.util.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JavaScriptSnippetCollection extends SnippetCollection {

  @Inject
  ClosestObjectFinder closestObjectFinder;

  @Inject
  public JavaScriptSnippetCollection(Snippets.Factory snippetsFactory) {
    initialize(snippetsFactory);
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_JAVASCRIPT;
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
    return defaultAttributeMlSnippet(trigger);
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
        (parent, matches) -> "constructor() {<%cursor%>\n}"
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
    return defaultEmptyTagMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> entrySnippet(SnippetTrigger trigger) {
    return defaultEntryMlSnippet(trigger);
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
    return Optional.of(
      snippets.addToTopLevelStatementListWithMlSnippet(trigger, Ast.Function.class)
    );
  }

  @Override
  protected Optional<Snippet> functionInlineSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Function.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            moveOptionalTypeToParensColonPostfix(matches, "function <%identifier%> {\n}")
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
    return defaultImplementsMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> importSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        JavaScriptAst.Import.class,
        (parent, matches) ->
          JavaScriptAst.Import.build(
            "<%identifier%>",
            matches.get("default") != null,
            optionalSlot(matches, "fromright"),
            optionalSlot(matches, "alias")
          ),
        Map.of(
          "fromright",
          Arrays.asList(FormattedTextOptions.newCamelCaseIdentifier()),
          "identifier",
          Arrays.asList(FormattedTextOptions.newCamelCaseIdentifier()),
          "alias",
          Arrays.asList(FormattedTextOptions.newCamelCaseIdentifier())
        )
      )
    );
  }

  @Override
  protected SnippetTrigger importTrigger() {
    return new SnippetTrigger(
      "import( <%default:default%>)? <%identifier%>( as <%alias%>)?( from <%fromright%>)?"
    );
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
        (parent, matches) -> "console.log(<%expression%><%cursor%>)<%terminator%>"
      )
    );
  }

  @Override
  protected Optional<Snippet> propertySnippet(SnippetTrigger trigger) {
    // This is an ML snippet that adds the snippet either as a Property or KeyValuePair.
    return Optional.of(
      snippets.snippet(
        trigger,
        parent ->
          parent instanceof Ast.KeyValuePairList ? new Ast.KeyValuePair() : new Ast.Property(),
        (source, cursor, root) -> {
          AstListWithLinePadding list;
          try {
            list =
              closestObjectFinder.closestList(
                root,
                source,
                root
                  .find(AstListWithLinePadding.class)
                  .filter(
                    e ->
                      e instanceof Ast.KeyValuePairList ||
                      e instanceof Ast.ClassMemberList ||
                      e instanceof Ast.InterfaceMemberList
                  ),
                cursor
              );
          } catch (ObjectNotFound e) {
            throw new CannotFindInsertionPoint();
          }

          return list;
        },
        (c, m) -> "<%snippet%>",
        snippets::addToListInstance,
        Map.of(),
        /* mlSnippet */true
      )
    );
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
    return defaultTagMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> trySnippet(SnippetTrigger trigger) {
    return defaultTryMlSnippet(trigger);
  }

  // because "type" also refers to type aliases in typescript, handle this manually in extraSnippets
  @Override
  protected Optional<Snippet> typeSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> whileSnippet(SnippetTrigger trigger) {
    return defaultWhileMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> withSnippet(SnippetTrigger trigger) {
    return defaultWithMlSnippet(trigger);
  }

  @Override
  protected List<Snippet> extraSnippets() {
    return Arrays.asList(
      snippets.addToList(
        new SnippetTrigger("import star( as <%alias%>)?( from <%fromright%>)?"),
        JavaScriptAst.Import.class,
        (parent, matches) ->
          JavaScriptAst.Import.build(
            "star",
            false,
            optionalSlot(matches, "fromright"),
            optionalSlot(matches, "alias")
          ),
        Map.of(
          "fromright",
          Arrays.asList(FormattedTextOptions.newCamelCaseIdentifier()),
          "identifier",
          Arrays.asList(FormattedTextOptions.newCamelCaseIdentifier()),
          "alias",
          Arrays.asList(FormattedTextOptions.newCamelCaseIdentifier())
        )
      ),
      snippets.addToList(
        new SnippetTrigger("object function <%identifier%>"),
        Ast.KeyValuePair.class,
        (parent, matches) ->
          Ast.KeyValuePair.build("<%identifier%><%cursor%>", Optional.of("function() {\n}")),
        Map.of("identifier", Arrays.asList(FormattedTextOptions.newCamelCaseExpression()))
      ),
      snippets.addToList(
        new SnippetTrigger("type <%identifier%> equals <%expression%>"),
        Ast.Statement.class,
        (parent, matches) -> {
          matches.put(
            "identifier",
            matches.get("identifier") + " equals " + matches.get("expression")
          );
          matches.remove("expression");
          return "type <%identifier%><%cursor%><%terminator%>";
        }
      ),
      // make sure "add type foo equals bar" takes priority over "add type foo"
      snippets.addToOptional(
        new SnippetTrigger("type <%type%>"),
        Ast.Type.class,
        (parent, matches) -> "<%type%>"
      ),
      snippets.addToList(
        new SnippetTrigger("modifiers <%expression%>", "modifiers"),
        Ast.Modifier.class,
        (parent, matches) -> "<%expression%>"
      )
    );
  }
}
