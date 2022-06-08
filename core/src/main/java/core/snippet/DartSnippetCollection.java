package core.snippet;

import core.ast.Ast;
import core.ast.DartAst;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DartSnippetCollection extends SnippetCollection {

  @Inject
  public DartSnippetCollection(Snippets.Factory snippetsFactory) {
    initialize(snippetsFactory);
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_DART;
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
          "on " +
          optionalSlot(matches, "expression").orElse("Exception") +
          " catch (e<%cursor%>) {\n}"
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
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Constructor.class,
        (parent, matches) -> {
          List<Ast.Class_> classAncestors = parent
            .ancestors(Ast.Class_.class)
            .collect(Collectors.toList());

          if (!classAncestors.isEmpty()) {
            return classAncestors.get(0).nameString() + "<%cursor%>() {\n}";
          }

          return "";
        }
      )
    );
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
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> enumSnippet(SnippetTrigger trigger) {
    return defaultEnumSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> extendsSnippet(SnippetTrigger trigger) {
    return defaultExtendsSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> finallySnippet(SnippetTrigger trigger) {
    return defaultFinallySnippet(trigger);
  }

  @Override
  protected Optional<Snippet> forSnippet(SnippetTrigger trigger) {
    return defaultForSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> forEachSnippet(SnippetTrigger trigger) {
    return defaultForEachSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> functionSnippet(SnippetTrigger trigger) {
    return defaultFunctionSnippet(trigger);
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
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Import.class,
        (parent, matches) -> "import \"<%identifier%><%cursor%>\"<%terminator%>"
      )
    );
  }

  @Override
  protected Optional<Snippet> interfaceSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> methodSnippet(SnippetTrigger trigger) {
    return defaultMethodSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> modifierSnippet(SnippetTrigger trigger) {
    return defaultModifierSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> parameterSnippet(SnippetTrigger trigger) {
    return defaultParameterSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> printSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Statement.class,
        (parent, matches) -> "print(<%expression%><%cursor%>)<%terminator%>"
      )
    );
  }

  @Override
  protected Optional<Snippet> propertySnippet(SnippetTrigger trigger) {
    return defaultPropertySnippet(trigger);
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
          optionalSlot(matches, "expression")
            .map(e -> "<%indent%>" + e + "<%terminator%>\n")
            .orElse("") +
          "} on Exception catch (e) {\n}"
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
    return Optional.of(
      snippets.addToListOptional(
        trigger,
        DartAst.MixinType.class,
        () -> new DartAst.MixinList(),
        (parent, matches) -> "<%identifier%>"
      )
    );
  }

  @Override
  protected SnippetTrigger withTrigger() {
    return new SnippetTrigger("with <%identifier%>");
  }

  @Override
  protected List<Snippet> extraSnippets() {
    return Arrays.asList(
      snippets.addToList(
        new SnippetTrigger("assert <%expression%>"),
        Ast.Statement.class,
        (parent, matches) -> "assert(<%expression%><%cursor%>)<%terminator%>"
      ),
      snippets.addToList(
        new SnippetTrigger("extension <%identifier%> on <%type%>", "extension"),
        DartAst.Extension.class,
        (parent, matches) -> "extension <%identifier%> on <%type%><%cursor%> {\n}"
      ),
      snippets.addToListOptional(
        new SnippetTrigger(
          "named <%type%> parameter <%identifier%>( equals <%expression%>)?",
          "named parameter"
        ),
        Ast.Parameter.class,
        () -> new DartAst.NamedParameterList(),
        (parent, matches) -> moveOptionalTypeToPrefix(matches, "<%identifier%>")
      ),
      snippets.addToListOptional(
        new SnippetTrigger("positional <%type%> parameter <%identifier%>"),
        Ast.Parameter.class,
        () -> new DartAst.PositionalParameterList(),
        (parent, matches) -> moveOptionalTypeToPrefix(matches, "<%identifier%>")
      ),
      snippets.addToList(
        new SnippetTrigger("mixin <%identifier%>( on <%type%>)?"),
        DartAst.Mixin.class,
        (parent, matches) ->
          "mixin <%identifier%>" +
          optionalSlot(matches, "type").map(e -> " on " + e).orElse("") +
          "<%cursor%> {\n}"
      )
    );
  }
}
