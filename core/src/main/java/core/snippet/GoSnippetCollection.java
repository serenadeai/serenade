package core.snippet;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.GoAst;
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
public class GoSnippetCollection extends SnippetCollection {

  @Inject
  public GoSnippetCollection(Snippets.Factory snippetsFactory) {
    initialize(snippetsFactory);
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_GO;
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
    return Optional.empty();
  }

  @Override
  protected SnippetTrigger classTrigger() {
    return new SnippetTrigger("struct <%identifier%>", "struct");
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
    return defaultForMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> forEachSnippet(SnippetTrigger trigger) {
    return defaultForEachMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> functionSnippet(SnippetTrigger trigger) {
    return defaultFunctionMlSnippet(trigger);
  }

  @Override
  protected SnippetTrigger functionInlineTrigger() {
    return new SnippetTrigger("inline( <%type%>)? function", "function");
  }

  @Override
  protected Optional<Snippet> functionInlineSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Function.class,
        (parent, matches) -> {
          if (matches.get("type") == null) {
            return "func<%cursor%>() {\n}";
          }
          return "func<%cursor%>() <%type%> {\n}";
        }
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
    return Optional.empty();
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
        (parent, matches) -> "fmt.Printf(<%expression%>)"
      )
    );
  }

  @Override
  protected SnippetTrigger propertyTrigger() {
    return new SnippetTrigger("<%type%> (property|field|member) <%identifier%>", "property");
  }

  @Override
  protected Optional<Snippet> propertySnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Property.class,
        (parent, matches) -> "<%identifier%><%cursor%> <%type%>"
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
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> trySnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> typeSnippet(SnippetTrigger trigger) {
    return defaultTypeMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> withSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> whileSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected List<Snippet> extraSnippets() {
    return Arrays.asList(
      snippets.addToOptional(
        new SnippetTrigger("receiver argument <%identifier%> <%type%>", "receiver argument"),
        Ast.ReceiverArgument.class,
        (parent, matches) -> "<%identifier%> <%type%>"
      )
    );
  }
}
