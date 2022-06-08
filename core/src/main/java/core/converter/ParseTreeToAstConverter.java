package core.converter;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.api.AstList;
import core.ast.api.AstListOptional;
import core.ast.api.AstListWithLinePadding;
import core.ast.api.AstNode;
import core.ast.api.AstOptional;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import core.language.LanguageSpecific;
import core.parser.ParseTree;
import core.util.Range;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import toolbelt.languages.LanguageDeterminer;

public abstract class ParseTreeToAstConverter implements LanguageSpecific {

  protected class Inlined extends DefaultAstParent {}

  private final List<String> commentPrefixes = Arrays.asList("#", "//");
  private final Map<String, String> commentEnclosures = Map.of("/*", "*/", "<!--", "-->");
  private final Map<String, String> stringEnclosures = Map.of(
    "`",
    "`",
    "```",
    "```",
    "\"\"\"",
    "\"\"\"",
    "\"",
    "\"",
    "'",
    "'",
    "'''",
    "'''"
  );

  protected Map<String, BiFunction<String, ParseTree, AstNode>> typeToConverter = new HashMap<>();

  @Inject
  Whitespace whitespace;

  public abstract Language language();

  protected abstract void registerConverters();

  protected void initialize() {
    // Converters to note:
    // Assignments have some nested layers automatically added via converters, so that we do not need
    //   to add all those layers in the grammar, since it might be tricky. We also extract out equals sign
    //   since it's not feasible to edit in some grammars.
    // Statements inline EnclosedBody since sometimes grammars allow those as a statement type.
    register("anonymous_property", () -> new Ast.AnonymousProperty());
    register("anonymous_property_list", () -> new Ast.AnonymousPropertyList());
    register("argument", () -> new Ast.Argument());
    register("argument_list", () -> new Ast.ArgumentList());
    register("argument_list_optional", () -> new Ast.ArgumentListOptional());
    register("array_pattern_list", () -> new Ast.ArrayPatternList());
    register("assert", () -> new Ast.Assert());
    register(
      "assignment",
      () -> new Ast.Assignment(),
      fixer ->
        fixer
          .wrapSublists(() -> new Ast.AssignmentVariableList())
          .splitLeadingEqualsFromChild(matches(Ast.AssignmentValue.class))
          .wrapSublists(() -> new Ast.AssignmentValueList())
          .splitLeadingEqualsFromChild(matches(Ast.AssignmentValueList.class))
          .wrapIndividually(
            matches(Ast.AssignmentValueList.class),
            () -> new Ast.AssignmentValueListOptional()
          )
    );
    register("assignment_list", () -> new Ast.AssignmentList());
    register("assignment_value", () -> new Ast.AssignmentValue());
    register("assignment_value_list", () -> new Ast.AssignmentValueList());
    register(
      "assignment_value_list_optional",
      () -> new Ast.AssignmentValueListOptional(),
      fixer ->
        fixer
          .splitLeadingEqualsFromChild(matches(Ast.AssignmentValue.class))
          .wrapSublists(() -> new Ast.AssignmentValueList())
          .splitLeadingEqualsFromChild(matches(Ast.AssignmentValueList.class))
    );
    register("assignment_variable", () -> new Ast.AssignmentVariable());
    register("assignment_variable_list", () -> new Ast.AssignmentVariableList());
    register("begin_clause", () -> new Ast.BeginClause());
    register("begin", () -> new Ast.Begin());
    register("block_collection", () -> new Ast.BlockCollection());
    register("block_initializer", () -> new Ast.BlockInitializer());
    register("block_initializer_optional", () -> new Ast.BlockInitializerOptional());
    register("block_iterator", () -> new Ast.BlockIterator());
    register("block_update", () -> new Ast.BlockUpdate());
    register("block_update_optional", () -> new Ast.BlockUpdateOptional());
    register("break", () -> new Ast.Break());
    register("call", () -> new Ast.Call());
    register("case", () -> new Ast.Case());
    register("catch", () -> new Ast.CatchClause());
    register("catch_filter", () -> new Ast.CatchFilter());
    register("catch_filter_optional", () -> new Ast.CatchFilterOptional());
    register("catch_list", () -> new Ast.CatchClauseList());
    register("catch_parameter", () -> new Ast.CatchParameter());
    register("catch_parameter_optional", () -> new Ast.CatchParameterOptional());
    register("class", () -> new Ast.Class_());
    register("class_member_list", () -> new Ast.ClassMemberList());
    register(Arrays.asList("comment", "line_comment", "block_comment"), this::convertComment);
    register("comment_text", () -> new Ast.CommentText());
    register("condition", () -> new Ast.Condition());
    register("condition_optional", () -> new Ast.ConditionOptional());
    register("constructor", () -> new Ast.Constructor());
    register("continue", () -> new Ast.Continue());
    register("css_include", () -> new Ast.CssInclude());
    register("css_media", () -> new Ast.CssMedia());
    register("css_mixin", () -> new Ast.CssMixin());
    register("css_ruleset", () -> new Ast.CssRuleset());
    register("css_selector", () -> new Ast.CssSelector());
    register("css_selector_list", () -> new Ast.CssSelectorList());
    register("debugger", () -> new Ast.Debugger());
    register("decorator", () -> new Ast.Decorator());
    register("decorator_list", () -> new Ast.DecoratorList());
    register("decorator_expression", () -> new Ast.DecoratorExpression());
    register("defer", () -> new Ast.Defer());
    register("dictionary", () -> new Ast.Dictionary());
    register("do_while", () -> new Ast.DoWhile());
    register("else_clause", () -> new Ast.ElseClause());
    register("else_clause_list", () -> new Ast.ElseClauseList());
    register("else_clause_optional", () -> new Ast.ElseClauseOptional());
    register("else_if_clause", () -> new Ast.ElseIfClause());
    register("else_if_clause_list", () -> new Ast.ElseIfClauseList());
    register("enclosed_body", () -> new Ast.EnclosedBody());
    register("ensure_clause", () -> new Ast.EnsureClause());
    register("ensure_clause_list", () -> new Ast.EnsureClauseList());
    register("enum", () -> new Ast.Enum());
    register("enum_constant", () -> new Ast.EnumConstant());
    register("enum_member_list", () -> new Ast.EnumMemberList());
    register("extends_list", () -> new Ast.ExtendsList());
    register("extends_list_optional", () -> new Ast.ExtendsListOptional());
    register("extends_optional", () -> new Ast.ExtendsOptional());
    register("extends_type", () -> new Ast.ExtendsType());
    register("field_initializer", () -> new Ast.FieldInitializer());
    register("field_initializer_list", () -> new Ast.FieldInitializerList());
    register("finally_clause", () -> new Ast.FinallyClause());
    register("finally_clause_optional", () -> new Ast.FinallyClauseOptional());
    register("for", () -> new Ast.For());
    register("for_clause", () -> new Ast.ForClause());
    register("for_each_clause", () -> new Ast.ForEachClause());
    register("for_each_separator", () -> new Ast.ForEachSeparator());
    register("function", () -> new Ast.Function());
    register("function_modifier_list", () -> new Ast.FunctionModifierList());
    register("generator", () -> new Ast.Generator());
    register("identifier", () -> new Ast.Identifier());
    register("if", () -> new Ast.If());
    register("if_clause", () -> new Ast.IfClause());
    register("implementation", () -> new Ast.Implementation());
    register("implementation_member_list", () -> new Ast.ImplementationMemberList());
    register("implements_list", () -> new Ast.ImplementsList());
    register("implements_list_optional", () -> new Ast.ImplementsListOptional());
    register("implements_type", () -> new Ast.ImplementsType());
    register("import", () -> new Ast.Import());
    register("import_list", () -> new Ast.ImportList());
    register("import_specifier", () -> new Ast.ImportSpecifier());
    register("import_specifier_list", () -> new Ast.ImportSpecifierList());
    register("initializer_expression", () -> new Ast.InitializerExpression());
    register("initializer_expression_block", () -> new Ast.EnclosedBody());
    register("initializer_expression_list", () -> new Ast.InitializerExpressionList());
    register("interface", () -> new Ast.Interface());
    register("interface_member_list", () -> new Ast.InterfaceMemberList());
    register("jsx_embedded_expression", () -> new Ast.JsxEmbeddedExpression());
    register("key_value_pair", () -> new Ast.KeyValuePair());
    register("key_value_pair_key", () -> new Ast.KeyValuePairKey());
    register("key_value_pair_list", () -> new Ast.KeyValuePairList());
    register("key_value_pair_value", () -> new Ast.KeyValuePairValue());
    register("lambda", () -> new Ast.Lambda());
    register("list", () -> new Ast.List_());
    register("list_element", () -> new Ast.ListElement());
    register("loop", () -> new Ast.Loop());
    register("loop_label_optional", () -> new Ast.LoopLabelOptional());
    register("markup_attribute", () -> new Ast.MarkupAttribute());
    register("markup_attribute_list", () -> new Ast.MarkupAttributeList());
    register("markup_attribute_name", () -> new Ast.MarkupAttributeName());
    register("markup_attribute_value", () -> new Ast.MarkupAttributeValue());
    register("markup_closing_tag", () -> new Ast.MarkupClosingTag());
    register("markup_content", () -> new Ast.MarkupContent());
    register("markup_content_list", () -> new Ast.MarkupContentList());
    register(Arrays.asList("jsx_fragment", "markup_element"), () -> new Ast.MarkupElement());
    register("markup_opening_tag", () -> new Ast.MarkupOpeningTag());
    register("markup_singleton_tag", () -> new Ast.MarkupSingletonTag());
    register("markup_tag", () -> new Ast.MarkupTag());
    register("markup_text", () -> new Ast.MarkupText());
    register(
      "member",
      () -> new Ast.Member(),
      fixer ->
        fixer
          .changeChildType(() -> new Ast.Function(), () -> new Ast.Method())
          .changeChildType(() -> new Ast.VariableDeclaration(), () -> new Ast.Property())
    );
    register("method", () -> new Ast.Method());
    register("modifier", () -> new Ast.Modifier());
    register("modifier_list", () -> new Ast.ModifierList());
    register("namespace", () -> new Ast.Namespace());
    register("object_pattern_list", () -> new Ast.ObjectPatternList());
    register("package", () -> new Ast.Package());
    register("package_optional", () -> new Ast.PackageOptional());
    register("parameter", () -> new Ast.Parameter());
    register("parameter_list", () -> new Ast.ParameterList());
    register("parameter_list_optional", () -> new Ast.ParameterListOptional());
    register("parameter_value", () -> new Ast.ParameterValue());
    register("pattern_list_element", () -> new Ast.PatternListElement());
    register("placeholder", () -> new Ast.Placeholder());
    // If there are errors in the parse, make sure we have at least a top-level statement list.
    // Override this in language converters if they have subclassed StatementLists.
    register(
      "program",
      () -> new Ast.Program(),
      fixer ->
        fixer.wrap(
          parent ->
            !parent.children().stream().anyMatch(child -> child instanceof Ast.StatementList),
          () -> new Ast.StatementList()
        )
    );
    register("property", () -> new Ast.Property());
    register("property_tag", () -> new Ast.PropertyTag());
    register("property_tag_optional", () -> new Ast.PropertyTagOptional());
    register("receiver_argument", () -> new Ast.ReceiverArgument());
    register("receiver_argument_optional", () -> new Ast.ReceiverArgumentOptional());
    register("rescue_clause", () -> new Ast.RescueClause());
    register("rescue_clause_list", () -> new Ast.RescueClauseList());
    register("rescue_else_ensure_element", () -> new Ast.RescueElseEnsureElement());
    register("rescue_parameter", () -> new Ast.RescueParameter());
    register("rescue_parameter_optional", () -> new Ast.RescueParameterOptional());
    register("rest_argument", () -> new Ast.RestArgument());
    register("rest_argument_optional", () -> new Ast.RestArgumentOptional());
    register("rest_parameter", () -> new Ast.RestParameter());
    register("rest_parameter_optional", () -> new Ast.RestParameterOptional());
    register("return", () -> new Ast.Return());
    register("return_type_optional", () -> new Ast.ReturnTypeOptional());
    register("return_value", () -> new Ast.ReturnValue());
    register("return_value_name", () -> new Ast.ReturnValueName());
    register("return_value_name_list", () -> new Ast.ReturnValueNameList());
    register("return_value_name_list_optional", () -> new Ast.ReturnValueNameListOptional());
    register("return_value_optional", () -> new Ast.ReturnValueOptional());
    register("set", () -> new Ast.Set_());
    register("splat", () -> new Ast.Splat());
    register(
      "statement",
      () -> new Ast.Statement(),
      fixer ->
        fixer.setType(
          parent ->
            parent
              .children()
              .stream()
              .anyMatch(child -> child.getClass().equals(Ast.EnclosedBody.class)),
          () -> new Inlined()
        )
    );
    register("statement_list", () -> new Ast.StatementList());
    register(
      Arrays.asList("string", "string_literal", "character_literal", ""),
      this::convertString
    );
    register("string_text", () -> new Ast.StringText());
    register("struct", () -> new Ast.Struct());
    register("struct_member_list", () -> new Ast.StructMemberList());
    register("switch", () -> new Ast.Switch());
    register("switch_default", () -> new Ast.SwitchDefault());
    register("synchronized", () -> new Ast.Synchronized());
    register("text", () -> new Ast.Text());
    register("throw", () -> new Ast.Throw());
    register("throws_optional", () -> new Ast.ThrowsOptional());
    register("throws_type", () -> new Ast.ThrowsType());
    register("trait", () -> new Ast.Trait());
    register("trait_bounds_optional", () -> new Ast.TraitBoundsOptional());
    register("trait_member_list", () -> new Ast.TraitMemberList());
    register("try", () -> new Ast.Try());
    register("try_clause", () -> new Ast.TryClause());
    register("tuple", () -> new Ast.Tuple());
    register("type", () -> new Ast.Type());
    register("type_alias", () -> new Ast.TypeAlias());
    register("type_argument", () -> new Ast.TypeArgument());
    register("type_argument_list", () -> new Ast.TypeArgumentList());
    register("type_declaration_list", () -> new Ast.TypeDeclarationList());
    register("type_decorator_list", () -> new Ast.TypeDecoratorList());
    register("type_optional", () -> new Ast.TypeOptional());
    register(
      Arrays.asList("type_parameter", "constrained_type_parameter"),
      () -> new Ast.TypeParameter()
    );
    register("type_parameter_constraint_list", () -> new Ast.TypeParameterConstraintList());
    register(
      "type_parameter_constraint_list_optional",
      () -> new Ast.TypeParameterConstraintListOptional()
    );
    register("type_parameter_constraint_type", () -> new Ast.TypeParameterConstraintType());
    register("type_parameter_list", () -> new Ast.TypeParameterList());
    register("type_parameter_list_optional", () -> new Ast.TypeParameterListOptional());
    register("until", () -> new Ast.Until());
    register("until_clause", () -> new Ast.UntilClause());
    register("using", () -> new Ast.Using());
    register("use_clause", () -> new Ast.UseClause());
    register("use_clause_list", () -> new Ast.UseClauseList());
    register(
      "variable_declaration",
      () -> new Ast.VariableDeclaration(),
      fixer -> fixer.wrapIndividually(matches(Ast.Assignment.class), () -> new Ast.AssignmentList())
    );
    register("while", () -> new Ast.While());
    register("while_clause", () -> new Ast.WhileClause());
    register("with", () -> new Ast.With());
    register("with_item", () -> new Ast.WithItem());
    register("with_item_alias", () -> new Ast.WithItemAlias());
    register("with_item_alias_optional", () -> new Ast.WithItemAliasOptional());
    register("with_item_list", () -> new Ast.WithItemList());
    register("with_item_value", () -> new Ast.WithItemValue());

    if (bracesOnSameLine()) {
      register("catch_list", () -> new Ast.CatchClauseListWithBracesOnSameLine());
      register("else_clause_optional", () -> new Ast.ElseClauseOptionalWithBracesOnSameLine());
      register("else_if_clause_list", () -> new Ast.ElseIfClauseListWithBracesOnSameLine());
      register(
        "finally_clause_optional",
        () -> new Ast.FinallyClauseOptionalWithBracesOnSameLine()
      );
    }

    registerConverters();
  }

  protected boolean bracesOnSameLine() {
    return false;
  }

  public List<Ast.Comment> convertComments(String source, ParseTree root) {
    List<Ast.Comment> comments = new ArrayList<>();
    convertComments(source, root, comments);
    return comments;
  }

  protected void convertComments(String source, ParseTree root, List<Ast.Comment> comments) {
    // We also pre-process comments in the conversion from tree-sitter -> ParseTree.
    // If the language grammar does not call these nodes `comment`, both parts will break.
    if (Arrays.asList("comment", "line_comment", "block_comment").contains(root.getType())) {
      Ast.Comment comment = (Ast.Comment) convert(source, root);
      comments.add(comment);
      return;
    }

    for (ParseTree child : root.getChildren()) {
      convertComments(source, child, comments);
    }
  }

  protected List<AstNode> convertChildren(String source, ParseTree node) {
    return node
      .getChildren()
      .stream()
      .filter(
        c ->
          !(
            c.getType().equals("ERROR") ||
            c.getType().equals("comment") ||
            c.getType().equals("line_comment") ||
            c.getType().equals("block_comment") ||
            c.getType().equals("\n") || // shows up in bash for some reason.
            c.getType().equals("preprocessor_call") // hide these in csharp.
          )
      )
      .flatMap(
        c -> {
          AstNode childNode = convert(source, c);
          if (childNode instanceof Inlined) {
            return childNode.children().stream();
          }
          return Stream.of(childNode);
        }
      )
      .collect(Collectors.toList());
  }

  protected <T extends AstToken> T createToken(T token, String source, ParseTree node) {
    return createToken(token, source, new Range(node.getStart(), node.getStop()));
  }

  protected <T extends AstToken> T createToken(T token, String source, Range range) {
    token.range = range;
    token.setCode(source.substring(range.start, range.stop));
    return token;
  }

  protected <T extends AstParent> T createWithConvertedChildren(
    T parent,
    String source,
    ParseTree node
  ) {
    if (node.getChildren().size() == 0) {
      parent.setChildren(
        convertNonWhitespaceToTokens(
          () -> new AstToken(),
          source,
          new Range(node.getStart(), node.getStop())
        )
      );
    } else {
      parent.setChildren(convertChildren(source, node));
    }
    return parent;
  }

  protected void register(List<String> types, Supplier<? extends AstParent> parentFactory) {
    for (String type : types) {
      register(type, parentFactory);
    }
  }

  protected void register(
    List<String> types,
    Supplier<? extends AstParent> parentFactory,
    Consumer<Fixer> fix
  ) {
    for (String type : types) {
      register(type, parentFactory, fix);
    }
  }

  protected void register(
    String type,
    Supplier<? extends AstParent> parentFactory,
    Consumer<Fixer> fix
  ) {
    typeToConverter.put(
      type,
      (s, n) -> {
        Fixer fixer = new Fixer(createWithConvertedChildren(parentFactory.get(), s, n));

        // Apply inlining of duplicated nodes. This needs to happen prior to the other fixers.
        fixer.inline(matches(parentFactory.get().getClass()));

        fix.accept(fixer);
        return fixer.parent;
      }
    );
  }

  protected void register(String type, Supplier<? extends AstParent> parentFactory) {
    register(type, parentFactory, fixer -> {});
  }

  protected void register(List<String> types, BiFunction<String, ParseTree, AstNode> converter) {
    for (String type : types) {
      register(type, converter);
    }
  }

  protected void register(String type, BiFunction<String, ParseTree, AstNode> converter) {
    typeToConverter.put(type, converter);
  }

  protected List<AstNode> replace(List<AstNode> children, AstParent parent, int index) {
    List<AstNode> result = new ArrayList<>(children);
    parent.setChildren(children.get(index).children());
    result.set(index, parent);
    return result;
  }

  protected AstNode convertText(
    AstParent parent,
    AstParent text,
    Map<String, String> enclosures,
    List<String> prefixes,
    String source,
    ParseTree node
  ) {
    // skip string prefix in pythnon:
    // https://docs.python.org/3/reference/lexical_analysis.html#string-literals
    int i = node.getStart();
    while (i < node.getStop() && Character.isLetter(source.charAt(i))) {
      i++;
    }
    int start = i;
    String string = source.substring(start, node.getStop());
    Optional<String> enclosureStart = enclosures
      .keySet()
      .stream()
      .filter(e -> string.startsWith(e))
      .findFirst();
    Optional<String> postfix = Optional.empty();
    // empty if we get some weird unicode version or something. we don't
    // care enough (it's mainly used by CorpusGen) about this node to fail hard here.
    Optional<String> prefix = Optional.empty();
    if (enclosureStart.isPresent()) {
      prefix = enclosureStart;
      postfix = Optional.of(enclosures.get(prefix.get()));
    } else {
      prefix = prefixes.stream().filter(e -> string.startsWith(e)).findFirst();
    }
    Optional<AstToken> startToken = prefix.map(
      p -> createToken(new AstToken(), source, new Range(node.getStart(), start + p.length()))
    );
    Optional<AstToken> stopToken = postfix.map(
      p ->
        createToken(new AstToken(), source, new Range(node.getStop() - p.length(), node.getStop()))
    );
    parent.setChildren(
      Arrays
        .asList(startToken.stream(), Stream.of(text), stopToken.stream())
        .stream()
        .flatMap(e -> e)
        .collect(Collectors.toList())
    );
    text.setChildren(
      convertNonWhitespaceToTokens(
        () -> new AstToken(),
        source,
        new Range(
          start + prefix.map(t -> t.length()).orElse(0),
          node.getStop() - postfix.map(t -> t.length()).orElse(0)
        )
      )
    );
    return parent;
  }

  protected AstNode convertComment(String source, ParseTree node) {
    return convertText(
      new Ast.Comment(),
      new Ast.CommentText(),
      commentEnclosures,
      commentPrefixes,
      source,
      node
    );
  }

  protected AstNode convertString(String source, ParseTree node) {
    // handle tree-sitter bug where node type of string keyword is the same.
    if (node.getChildren().size() == 0) {
      return createToken(new AstToken(), source, node);
    }

    return convertText(
      new Ast.String_(),
      new Ast.StringText(),
      stringEnclosures,
      Collections.emptyList(),
      source,
      node
    );
  }

  public List<AstNode> convertNonWhitespaceToTokens(
    Supplier<? extends AstToken> tokenFactory,
    String source,
    Range range
  ) {
    return whitespace
      .lineRanges(source, range)
      .stream()
      .map(r -> whitespace.strip(source, r))
      .filter(r -> r.start != r.stop)
      .map(r -> createToken(tokenFactory.get(), source, r))
      .collect(Collectors.toList());
  }

  public AstNode convert(String source, ParseTree node) {
    AstNode result = null;

    BiFunction<String, ParseTree, AstNode> converter = typeToConverter.get(node.getType());

    // Tree-sitter string matches, e.g. for "enum", are translated into parse-trees of the form:
    // <enum>
    //   enum
    // </enum>
    // which have no children, and the type == the code.
    // However, these conflict with converter names and inadvertently trigger them.
    if (
      converter != null && node.getChildren().size() == 0 && node.getType().equals(node.getCode())
    ) {
      converter = (s, n) -> createToken(new AstToken(), s, n);
    }

    if (converter == null) {
      if (node.getChildren().size() > 0) {
        // We by default now inline every non-registered tree-sitter rule/field name.
        converter = (s, n) -> createWithConvertedChildren(new Inlined(), s, n);
      } else {
        converter =
          (s, n) -> {
            // When we haven't registered a converter on the node type, filter
            // whitespace tokens by inlining nodes with no children.
            if (whitespace.isWhitespace(n.getCode())) {
              return new Inlined();
            } else {
              return createToken(new AstToken(), s, n);
            }
          };
      }
    }

    result = converter.apply(source, node);

    if (result instanceof AstParent) {
      fixSemicolonsAndWrapChildren((AstParent) result);
    }

    result.setParseTree(Optional.of(node));
    return result;
  }

  public Set<String> registeredRules() {
    return typeToConverter.keySet();
  }

  protected Predicate<AstNode> matches(Class<?> type) {
    return e -> e.getClass().equals(type);
  }

  private void fixSemicolonsAndWrapChildren(AstParent node) {
    // Make sure WrapperElements are of the form
    // <Wrapper>
    //   <AstNode> ... </AstNode>
    //   <AstToken> ; </AstToken> (Optional)
    // </Wrapper>
    // We also make sure semicolons are located properly for some constructs.

    extractTrailingSemicolon(node, matches(Ast.BlockInitializerOptional.class));
    extractTrailingSemicolon(node, matches(Ast.ConditionOptional.class));

    if (node instanceof Ast.TerminatedElement) {
      long numberNonSemicolonChildren = node
        .children()
        .stream()
        .filter(child -> !child.isToken(";"))
        .count();
      if (numberNonSemicolonChildren > 1) {
        DefaultAstParent wrapper = new DefaultAstParent();
        wrapper.setChildren(node.children());
        node.setChildren(Arrays.asList(wrapper));
      }
      extractTrailingSemicolon(
        node,
        child ->
          !(child instanceof AstToken) &&
          node.children().indexOf(child) == node.children().size() - 1
      );
    } else if (node instanceof AstList) {
      mergeFollowingSemicolon(node, child -> child instanceof Ast.TerminatedElement);
    }
  }

  private void extractTrailingSemicolon(AstParent node, Predicate<AstNode> condition) {
    List<AstNode> matchingChildren = node
      .children()
      .stream()
      .filter(child -> condition.test(child))
      .collect(Collectors.toList());

    for (AstNode child : matchingChildren) {
      Optional<AstToken> token = child.rightMost(AstToken.class);
      if (token.isPresent() && token.get().code().equals(";")) {
        List<AstNode> children = token.get().parent().get().children();
        children.remove(token.get());

        List<AstNode> newChildren = new ArrayList<>(node.children());
        newChildren.add(node.children().indexOf(child) + 1, token.get());
        node.setChildren(newChildren);
      }
    }
  }

  private void mergeFollowingSemicolon(AstParent node, Predicate<AstNode> condition) {
    List<AstNode> children = new ArrayList<>();
    Optional<AstParent> previousMatchingChild = Optional.empty();
    for (AstNode child : node.children()) {
      if (child.isToken(";") && previousMatchingChild.isPresent()) {
        List<AstNode> previousGrandchildren = new ArrayList<>(
          previousMatchingChild.get().children()
        );
        previousGrandchildren.add(child);
        previousMatchingChild.get().setChildren(previousGrandchildren);
      } else {
        children.add(child);
      }
      if (child instanceof AstParent && condition.test(child)) {
        previousMatchingChild = Optional.of((AstParent) child);
      } else {
        previousMatchingChild = Optional.empty();
      }
    }
    node.setChildren(children);
  }
}
