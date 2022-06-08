package core.snippet;

import core.ast.Ast;
import core.ast.api.AstList;
import core.ast.api.AstListOptional;
import core.ast.api.AstNode;
import core.ast.api.AstOptional;
import core.ast.api.AstParent;
import core.codeengine.Resolver;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import core.language.LanguageSpecific;
import core.util.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

public abstract class SnippetCollection implements LanguageSpecific {

  @Inject
  Resolver resolver;

  private List<Snippet> snippetsList = new ArrayList<>();

  protected Snippets snippets;

  protected abstract Optional<Snippet> bareSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> argumentSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> attributeSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> catchSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> classSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> commentSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> constructorSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> decoratorSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> elementSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> elseSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> elseIfSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> emptyTagSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> entrySnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> enumSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> extendsSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> finallySnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> forSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> forEachSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> functionSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> functionInlineSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> ifSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> implementsSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> importSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> interfaceSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> methodSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> modifierSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> parameterSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> printSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> propertySnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> returnTypeSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> returnValueSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> tagSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> trySnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> typeSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> whileSnippet(SnippetTrigger trigger);

  protected abstract Optional<Snippet> withSnippet(SnippetTrigger trigger);

  public abstract Language language();

  protected void initialize(Snippets.Factory snippetsFactory) {
    snippets = snippetsFactory.create(language());

    snippetsList.addAll(extraSnippets());
    addToSnippets(argumentSnippet(argumentTrigger()));
    addToSnippets(attributeSnippet(attributeTrigger()));
    addToSnippets(catchSnippet(catchTrigger()));
    addToSnippets(classSnippet(classTrigger()));
    addToSnippets(commentSnippet(commentTrigger()));
    addToSnippets(constructorSnippet(constructorTrigger()));
    addToSnippets(decoratorSnippet(decoratorTrigger()));
    addToSnippets(elementSnippet(elementTrigger()));
    addToSnippets(elseIfSnippet(elseIfTrigger()));
    addToSnippets(elseSnippet(elseTrigger()));
    addToSnippets(emptyTagSnippet(emptyTagTrigger()));
    addToSnippets(entrySnippet(entryTrigger()));
    addToSnippets(enumSnippet(enumTrigger()));
    addToSnippets(extendsSnippet(extendsTrigger()));
    addToSnippets(finallySnippet(finallyTrigger()));
    addToSnippets(forEachSnippet(forEachTrigger()));
    addToSnippets(forSnippet(forTrigger()));
    addToSnippets(functionInlineSnippet(functionInlineTrigger()));
    addToSnippets(functionSnippet(functionTrigger()));
    addToSnippets(ifSnippet(ifTrigger()));
    addToSnippets(implementsSnippet(implementsTrigger()));
    addToSnippets(importSnippet(importTrigger()));
    addToSnippets(interfaceSnippet(interfaceTrigger()));
    addToSnippets(methodSnippet(methodTrigger()));
    addToSnippets(modifierSnippet(modifierTrigger()));
    addToSnippets(parameterSnippet(parameterTrigger()));
    addToSnippets(printSnippet(printTrigger()));
    addToSnippets(propertySnippet(propertyTrigger()));
    addToSnippets(returnTypeSnippet(returnTypeTrigger()));
    addToSnippets(returnValueSnippet(returnValueTrigger()));
    addToSnippets(tagSnippet(tagTrigger()));
    addToSnippets(trySnippet(tryTrigger()));
    addToSnippets(typeSnippet(typeTrigger()));
    addToSnippets(whileSnippet(whileTrigger()));
    addToSnippets(withSnippet(withTrigger()));
    addToSnippets(bareSnippet(bareTrigger()));
  }

  private void addToSnippets(Optional<Snippet> snippet) {
    snippet.ifPresent(s -> snippetsList.add(s));
  }

  protected String moveOptionalTypeToPostfix(
    Map<String, String> matches,
    String typeSlot,
    String identifierSlot,
    Optional<String> identifierPostfix,
    String typePrefix,
    String template
  ) {
    String identifier = matches.get(identifierSlot);
    Optional<String> equals = Optional.empty();
    if (identifier.contains("equal")) {
      int position = identifier.lastIndexOf("equal");
      equals = Optional.of(identifier.substring(position));
      identifier = identifier.substring(0, position - 1);
    }

    if (matches.get(typeSlot) == null) {
      matches.put(
        identifierSlot,
        identifier +
        resolver.wrapInSlot(resolver.cursorOverride) +
        identifierPostfix.map(e -> " " + e).orElse("") +
        equals.map(e -> " " + e).orElse("")
      );
      return template;
    }

    String type = matches.get(typeSlot);
    matches.put(
      identifierSlot,
      identifier +
      resolver.wrapInSlot(resolver.cursorOverride) +
      identifierPostfix.map(e -> " " + e).orElse("") +
      " " +
      typePrefix +
      " " +
      type +
      equals.map(e -> " " + e).orElse("")
    );

    matches.remove(typeSlot);
    return template;
  }

  protected String moveOptionalToPrefix(
    Map<String, String> matches,
    String optionalSlot,
    String destinationSlot,
    String template
  ) {
    if (matches.get(optionalSlot) != null) {
      matches.put(destinationSlot, matches.get(optionalSlot) + " " + matches.get(destinationSlot));
      matches.remove(optionalSlot);
    }

    return template;
  }

  protected String moveOptionalTypeToPrefix(Map<String, String> matches, String template) {
    return moveOptionalToPrefix(matches, "type", "identifier", template);
  }

  protected String prefixTemplateWithOptional(
    Map<String, String> matches,
    String slot,
    String template
  ) {
    String prefix = "";
    if (matches.get(slot) != null) {
      prefix = matches.get(slot) + " ";
      matches.remove(slot);
    }

    return prefix + template;
  }

  protected Optional<String> optionalSlot(Map<String, String> matches, String slot) {
    return matches.get(slot) == null ? Optional.empty() : Optional.of("<%" + slot + "%>");
  }

  protected String moveOptionalTypeToColonPostfix(Map<String, String> matches, String template) {
    return moveOptionalTypeToPostfix(
      matches,
      "type",
      "identifier",
      Optional.empty(),
      "colon",
      template
    );
  }

  protected String moveOptionalTypeToParensArrowPostfix(
    Map<String, String> matches,
    String template
  ) {
    return moveOptionalTypeToPostfix(
      matches,
      "type",
      "identifier",
      Optional.of("parens"),
      "arrow",
      template
    );
  }

  protected String moveOptionalTypeToParensColonPostfix(
    Map<String, String> matches,
    String template
  ) {
    return moveOptionalTypeToPostfix(
      matches,
      "type",
      "identifier",
      Optional.of("parens"),
      "colon",
      template
    );
  }

  protected SnippetTrigger bareTrigger() {
    return new SnippetTrigger("<%expression%>");
  }

  protected SnippetTrigger argumentTrigger() {
    return new SnippetTrigger("argument <%argument%>", "argument");
  }

  protected SnippetTrigger attributeTrigger() {
    return new SnippetTrigger("attribute <%attribute%>");
  }

  protected SnippetTrigger catchTrigger() {
    return new SnippetTrigger("(catch|except)( <%expression%>)?");
  }

  protected SnippetTrigger classTrigger() {
    return new SnippetTrigger("(<%modifiers%> )?class <%identifier%>", "class");
  }

  protected SnippetTrigger commentTrigger() {
    return new SnippetTrigger("comment( <%text%>)?");
  }

  protected SnippetTrigger constructorTrigger() {
    return new SnippetTrigger("constructor", "constructor");
  }

  protected SnippetTrigger decoratorTrigger() {
    return new SnippetTrigger("(annotation|decorator) <%expression%>", "decorator");
  }

  protected SnippetTrigger elementTrigger() {
    return new SnippetTrigger("element <%expression%>", "element");
  }

  protected SnippetTrigger elseTrigger() {
    return new SnippetTrigger("else( <%expression%>)?");
  }

  protected SnippetTrigger elseIfTrigger() {
    return new SnippetTrigger("(elif|else if)( <%condition%>)?");
  }

  protected SnippetTrigger emptyTagTrigger() {
    return new SnippetTrigger("empty tag <%identifier%>");
  }

  protected SnippetTrigger entryTrigger() {
    return new SnippetTrigger("entry <%expression%>", "entry");
  }

  protected SnippetTrigger enumTrigger() {
    return new SnippetTrigger("(<%modifiers%> )?enum <%identifier%>", "enum");
  }

  protected SnippetTrigger extendsTrigger() {
    return new SnippetTrigger("(extends|parent|superclass|base class) <%identifier%>", "extends");
  }

  protected SnippetTrigger finallyTrigger() {
    return new SnippetTrigger("finally( <%expression%>)?");
  }

  protected SnippetTrigger forTrigger() {
    return new SnippetTrigger("for");
  }

  protected SnippetTrigger forEachTrigger() {
    return new SnippetTrigger("for <%expression%>");
  }

  protected SnippetTrigger functionTrigger() {
    return new SnippetTrigger(
      "(<%modifiers%> )?(<%type%> )?(def|fun|function) <%identifier%>",
      "function"
    );
  }

  protected SnippetTrigger functionInlineTrigger() {
    return new SnippetTrigger(
      "inline( <%modifiers%>)?( <%type%>)? function <%identifier%>",
      "function"
    );
  }

  protected SnippetTrigger ifTrigger() {
    return new SnippetTrigger("if( <%condition%>)?");
  }

  protected SnippetTrigger implementsTrigger() {
    return new SnippetTrigger("implements <%identifier%>", "implements");
  }

  protected SnippetTrigger importTrigger() {
    return new SnippetTrigger("(import|include) <%identifier%>");
  }

  protected SnippetTrigger interfaceTrigger() {
    return new SnippetTrigger("(<%modifiers%> )?interface <%identifier%>", "interface");
  }

  protected SnippetTrigger methodTrigger() {
    return new SnippetTrigger("(<%modifiers%> )?(<%type%> )?method <%identifier%>");
  }

  protected SnippetTrigger modifierTrigger() {
    return new SnippetTrigger("modifier <%expression%>", "modifier");
  }

  protected SnippetTrigger parameterTrigger() {
    return new SnippetTrigger("(<%type%> )?parameter <%identifier%>", "parameter");
  }

  protected SnippetTrigger printTrigger() {
    return new SnippetTrigger("(print|console( dot)? log)( of)? <%expression%>");
  }

  protected SnippetTrigger propertyTrigger() {
    return new SnippetTrigger(
      "(<%modifiers%> )?(<%type%> )?(property|field|member) <%identifier%>",
      "property"
    );
  }

  protected SnippetTrigger returnTypeTrigger() {
    return new SnippetTrigger("return type <%type%>", "return type");
  }

  protected SnippetTrigger returnValueTrigger() {
    return new SnippetTrigger("return value <%expression%>", "return value");
  }

  protected SnippetTrigger tagTrigger() {
    return new SnippetTrigger("tag <%identifier%>");
  }

  protected SnippetTrigger tryTrigger() {
    return new SnippetTrigger("try( <%expression%>)?");
  }

  protected SnippetTrigger typeTrigger() {
    return new SnippetTrigger("type <%type%>", "type");
  }

  protected SnippetTrigger whileTrigger() {
    return new SnippetTrigger("while( loop)?( <%condition%>)?");
  }

  protected SnippetTrigger withTrigger() {
    return new SnippetTrigger("with <%expression%>");
  }

  protected Optional<Snippet> defaultBareSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addBasedOnParent(
        trigger,
        Map.of(
          "expression",
          Arrays.asList(
            FormattedTextOptions
              .newBuilder()
              .setExpression(true)
              .setStyle(TextStyle.CAMEL_CASE)
              .build()
          )
        )
      )
    );
  }

  protected Optional<Snippet> defaultBareMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addBasedOnParentWithMlSnippet(trigger));
  }

  protected Optional<Snippet> defaultArgumentSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Argument.class,
        (parent, matches) -> "<%argument%>",
        Map.of(
          "argument",
          Arrays.asList(
            FormattedTextOptions
              .newBuilder()
              .setExpression(true)
              .setStyle(TextStyle.CAMEL_CASE)
              .build()
          )
        )
      )
    );
  }

  protected Optional<Snippet> defaultArgumentMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Argument.class));
  }

  protected Optional<Snippet> defaultAttributeSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(trigger, Ast.MarkupAttribute.class, (parent, matches) -> "<%attribute%>")
    );
  }

  protected Optional<Snippet> defaultAttributeMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.MarkupAttribute.class));
  }

  protected Optional<Snippet> defaultBeginMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Begin.class));
  }

  protected Optional<Snippet> defaultCatchSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.CatchClause.class,
        (parent, matches) ->
          "catch (" + optionalSlot(matches, "expression").orElse("e") + "<%cursor%>) {\n}"
      )
    );
  }

  protected Optional<Snippet> defaultCatchMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.CatchClause.class));
  }

  protected Optional<Snippet> defaultClassSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Class_.class,
        (parent, matches) ->
          prefixTemplateWithOptional(matches, "modifiers", "class <%identifier%><%cursor%> {\n}")
      )
    );
  }

  protected Optional<Snippet> defaultClassMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Class_.class));
  }

  protected Optional<Snippet> defaultCommentSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addComment(trigger, Ast.Comment.class, (parent, matches) -> "// <%text%>")
    );
  }

  protected Optional<Snippet> defaultDecoratorSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Decorator.class,
        (parent, matches) -> "@<%expression%>",
        Map.of()
      )
    );
  }

  protected Optional<Snippet> defaultDecoratorMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Decorator.class));
  }

  protected Optional<Snippet> defaultElementSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(trigger, Ast.ListElement.class, (parent, matches) -> "<%expression%>")
    );
  }

  protected Optional<Snippet> defaultElementMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.ListElement.class));
  }

  protected Optional<Snippet> defaultElseSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToOptional(
        trigger,
        Ast.ElseClause.class,
        (parent, matches) ->
          "else<%cursor%> {\n" +
          optionalSlot(matches, "expression")
            .map(e -> "<%indent%>" + e + "<%terminator%>\n")
            .orElse("") +
          "}",
        Map.of()
      )
    );
  }

  protected Optional<Snippet> defaultElseMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToOptionalWithMlSnippet(trigger, Ast.ElseClause.class));
  }

  protected Optional<Snippet> defaultElseIfSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.ElseIfClause.class,
        (parent, matches) ->
          "else if (" + optionalSlot(matches, "condition").orElse("true") + "<%cursor%>) {\n}"
      )
    );
  }

  protected Optional<Snippet> defaultElseIfMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.ElseIfClause.class));
  }

  protected Optional<Snippet> defaultEmptyTagSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.MarkupElement.class,
        (parent, matches) -> "<<%identifier%> />"
      )
    );
  }

  protected Optional<Snippet> defaultEmptyTagMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.MarkupElement.class));
  }

  protected Optional<Snippet> defaultEntrySnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(trigger, Ast.KeyValuePair.class, (parent, matches) -> "<%expression%>")
    );
  }

  protected Optional<Snippet> defaultEntryMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.KeyValuePair.class));
  }

  protected Optional<Snippet> defaultEnumSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Enum.class,
        (parent, matches) ->
          prefixTemplateWithOptional(matches, "modifiers", "enum <%identifier%><%cursor%> {\n}")
      )
    );
  }

  protected Optional<Snippet> defaultEnumMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Enum.class));
  }

  protected Optional<Snippet> defaultExtendsSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToOptional(trigger, Ast.ExtendsType.class, (parent, matches) -> "<%identifier%>")
    );
  }

  protected Optional<Snippet> defaultExtendsMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToOptionalWithMlSnippet(trigger, Ast.ExtendsType.class));
  }

  protected Optional<Snippet> defaultExtendsListSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToListOptional(
        trigger,
        Ast.ExtendsType.class,
        () -> new Ast.ExtendsList(),
        (parent, matches) -> "<%identifier%>"
      )
    );
  }

  protected Optional<Snippet> defaultExtendsToListMlSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToListOptionalWithMlSnippet(
        trigger,
        Ast.ExtendsType.class,
        () -> new Ast.ExtendsList()
      )
    );
  }

  protected Optional<Snippet> defaultFinallySnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToOptional(
        trigger,
        Ast.FinallyClause.class,
        (parent, matches) ->
          "finally<%cursor%> {\n" +
          optionalSlot(matches, "expression")
            .map(e -> "<%indent%>" + e + "<%terminator%>")
            .orElse("") +
          "}"
      )
    );
  }

  protected Optional<Snippet> defaultFinallyMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToOptionalWithMlSnippet(trigger, Ast.FinallyClause.class));
  }

  protected Optional<Snippet> defaultForSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(trigger, Ast.For.class, (parent, matches) -> "for (<%cursor%>;;) {\n}")
    );
  }

  protected Optional<Snippet> defaultForMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.For.class));
  }

  protected Optional<Snippet> defaultForEachSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.For.class,
        (parent, matches) -> "for (<%expression%><%cursor%>) {\n}"
      )
    );
  }

  protected Optional<Snippet> defaultForEachMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.For.class));
  }

  protected Optional<Snippet> defaultFunctionSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Function.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            moveOptionalTypeToPrefix(matches, "<%identifier%><%cursor%>() {\n}")
          )
      )
    );
  }

  protected Optional<Snippet> defaultFunctionMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Function.class));
  }

  protected Optional<Snippet> defaultFunctionInlineSnippet(SnippetTrigger trigger) {
    return defaultFunctionSnippet(trigger);
  }

  protected Optional<Snippet> defaultIfSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.If.class,
        (parent, matches) ->
          "if (" + optionalSlot(matches, "condition").orElse("true") + "<%cursor%>) {\n}"
      )
    );
  }

  protected Optional<Snippet> defaultIfMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.If.class));
  }

  protected Optional<Snippet> defaultImplementsSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToListOptional(
        trigger,
        Ast.ImplementsType.class,
        () -> new Ast.ImplementsList(),
        (parent, matches) -> "<%identifier%>"
      )
    );
  }

  protected Optional<Snippet> defaultImplementsMlSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToListOptionalWithMlSnippet(
        trigger,
        Ast.ImplementsType.class,
        () -> new Ast.ImplementsList()
      )
    );
  }

  protected Optional<Snippet> defaultImportSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Import.class,
        (parent, matches) -> "import <%identifier%><%cursor%><%terminator%>"
      )
    );
  }

  protected Optional<Snippet> defaultImportMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Import.class));
  }

  protected Optional<Snippet> defaultInterfaceSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Interface.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            "interface <%identifier%><%cursor%> {\n}"
          )
      )
    );
  }

  protected Optional<Snippet> defaultInterfaceMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Interface.class));
  }

  protected Optional<Snippet> defaultMethodSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Method.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            moveOptionalTypeToPrefix(
              matches,
              "<%identifier%><%cursor%>()" +
              (parent instanceof Ast.ClassMemberList ? " {\n}" : "<%terminator%>")
            )
          )
      )
    );
  }

  protected Optional<Snippet> defaultMethodMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Method.class));
  }

  protected Optional<Snippet> defaultModifierSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(trigger, Ast.Modifier.class, (parent, matches) -> "<%expression%>")
    );
  }

  protected Optional<Snippet> defaultModifierMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Modifier.class));
  }

  protected Optional<Snippet> defaultParameterSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Parameter.class,
        (parent, matches) -> moveOptionalTypeToPrefix(matches, "<%identifier%>")
      )
    );
  }

  protected Optional<Snippet> defaultParameterMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Parameter.class));
  }

  protected Optional<Snippet> defaultPropertySnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Property.class,
        (parent, matches) ->
          prefixTemplateWithOptional(
            matches,
            "modifiers",
            moveOptionalTypeToPrefix(matches, "<%identifier%><%cursor%><%terminator%>")
          )
      )
    );
  }

  protected Optional<Snippet> defaultPropertyMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Property.class));
  }

  protected Optional<Snippet> defaultReturnTypeSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToOptional(trigger, Ast.Type.class, (parent, matches) -> "<%type%>")
    );
  }

  protected Optional<Snippet> defaultReturnTypeMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToOptionalWithMlSnippet(trigger, Ast.Type.class));
  }

  protected Optional<Snippet> defaultReturnValueSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToOptional(trigger, Ast.ReturnValue.class, (parent, matches) -> "<%expression%>")
    );
  }

  protected Optional<Snippet> defaultReturnValueMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToOptionalWithMlSnippet(trigger, Ast.ReturnValue.class));
  }

  protected Optional<Snippet> defaultTagSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.MarkupElement.class,
        (parent, matches) -> "<<%identifier%>><%cursor%></<%identifier%>>"
      )
    );
  }

  protected Optional<Snippet> defaultTagMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.MarkupElement.class));
  }

  protected Optional<Snippet> defaultTrySnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Try.class,
        (parent, matches) ->
          "try<%cursor%> {\n" +
          optionalSlot(matches, "expression")
            .map(e -> "<%indent%>" + e + "<%terminator%>\n")
            .orElse("") +
          "}"
      )
    );
  }

  protected Optional<Snippet> defaultTryMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Try.class));
  }

  protected Optional<Snippet> defaultTypeSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToOptional(trigger, Ast.Type.class, (parent, matches) -> "<%type%>")
    );
  }

  protected Optional<Snippet> defaultTypeMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToOptionalWithMlSnippet(trigger, Ast.Type.class));
  }

  protected Optional<Snippet> defaultWhileSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.While.class,
        (parent, matches) ->
          "while (" + optionalSlot(matches, "condition").orElse("true") + "<%cursor%>) {\n}"
      )
    );
  }

  protected Optional<Snippet> defaultWhileMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.While.class));
  }

  protected Optional<Snippet> defaultWithSnippet(SnippetTrigger trigger) {
    return Optional.of(
      snippets.addToList(
        trigger,
        Ast.Statement.class,
        (parent, matches) ->
          "with (" + optionalSlot(matches, "expression").orElse("true") + "<%cursor%>) {\n}"
      )
    );
  }

  protected Optional<Snippet> defaultWithMlSnippet(SnippetTrigger trigger) {
    return Optional.of(snippets.addToListWithMlSnippet(trigger, Ast.Statement.class));
  }

  protected List<Snippet> extraSnippets() {
    return Arrays.asList();
  }

  public List<Snippet> snippets() {
    return snippetsList;
  }
}
