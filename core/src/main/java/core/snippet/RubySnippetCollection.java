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
public class RubySnippetCollection extends SnippetCollection {

  @Inject
  public RubySnippetCollection(Snippets.Factory snippetsFactory) {
    initialize(snippetsFactory);
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_RUBY;
  }

  @Override
  protected Optional<Snippet> bareSnippet(SnippetTrigger trigger) {
    return defaultBareSnippet(trigger);
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
  protected Optional<Snippet> classSnippet(SnippetTrigger trigger) {
    return defaultClassMlSnippet(trigger);
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
        (parent, matches) -> "def initialize<%cursor%>\nend"
      )
    );
  }

  @Override
  protected Optional<Snippet> decoratorSnippet(SnippetTrigger trigger) {
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> elementSnippet(SnippetTrigger trigger) {
    return defaultElementMlSnippet(trigger);
  }

  @Override
  protected Optional<Snippet> elseSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToContainerWithMlSnippet(trigger, Ast.ElseClause.class));
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
    return defaultExtendsSnippet(trigger);
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
    return defaultFunctionMlSnippet(trigger);
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
    return Optional.empty();
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
    return Optional.empty();
  }

  @Override
  protected Optional<Snippet> parameterSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToListOptionalWithMlSnippet(
        trigger,
        Ast.Parameter.class,
        () -> new Ast.ParameterList()
      )
    );
  }

  @Override
  protected Optional<Snippet> printSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(trigger, Ast.Statement.class, (parent, matches) -> "print <%expression%>")
    );
  }

  @Override
  protected Optional<Snippet> propertySnippet(SnippetTrigger trigger) {
    return defaultPropertySnippet(trigger);
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
      snippets.addToListWithMlSnippet(
        new SnippetTrigger("begin( <%expression%>)?", "begin"),
        Ast.Begin.class
      ),
      snippets.addToListWithMlSnippet(
        new SnippetTrigger("ensure( <%expression%>)?", "ensure"),
        Ast.EnsureClause.class
      ),
      snippets.addToList(
        new SnippetTrigger("module <%identifier%>", "module"),
        Ast.Namespace.class,
        (parent, matches) -> "module <%identifier%><%cursor%>\nend"
      ),
      snippets.addToListWithMlSnippet(
        new SnippetTrigger("raise( <%expression%>)?", "raise <%expression%>"),
        Ast.Statement.class
      ),
      snippets.addToListWithMlSnippet(
        new SnippetTrigger("rescue( <%expression%>)?", "rescue"),
        Ast.RescueClause.class
      ),
      snippets.addToListWithMlSnippet(
        new SnippetTrigger("until( loop)?( <%condition%>)?", "until"),
        Ast.Until.class
      )
    );
  }
}
