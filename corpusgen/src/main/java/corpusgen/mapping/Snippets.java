package corpusgen.mapping;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.api.AstList;
import core.ast.api.AstNode;
import core.ast.api.AstToken;
import core.codeengine.Resolver;
import core.codeengine.Tokenizer;
import core.exception.SnippetGenerationException;
import core.formattedtext.ConversionMap;
import core.formattedtext.ConversionMapFactory;
import core.gen.rpc.Language;
import core.util.Range;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import toolbelt.languages.LanguageDeterminer;

public class Snippets {

  private final List<Class<? extends AstNode>> disabledSnippets = Arrays.asList();
  private final Map<Language, List<Class<? extends AstNode>>> languageSpecificDisabledSnippets = Map.of(
    Language.LANGUAGE_RUST,
    Arrays.asList()
  );

  private Map<Class<? extends AstNode>, BiFunction<GenerationContext, AstNode, Optional<Mapping>>> _snippets;

  protected AstFactory astFactory;
  protected ConversionMap conversionMap;
  protected Random random;
  protected SnippetGenerator snippetGenerator;

  protected Language language;
  protected LanguageDeterminer languageDeterminer;
  protected Function<GenerationContext, Mapping> generate;

  @AssistedInject
  public Snippets(
    AstFactory astFactory,
    ConversionMapFactory conversionMapFactory,
    LanguageDeterminer languageDeterminer,
    Random random,
    SnippetGenerator.Factory snippetGeneratorFactory,
    @Assisted Language language,
    @Assisted Function<GenerationContext, Mapping> generate
  ) {
    this.astFactory = astFactory;
    this.conversionMap = conversionMapFactory.create(language);
    this.languageDeterminer = languageDeterminer;
    this.random = random;
    this.snippetGenerator = snippetGeneratorFactory.create((g, n) -> generateFormattedText(g, n));
    this.language = language;
    this.generate = generate;
  }

  @AssistedFactory
  public interface Factory {
    Snippets create(Language language, Function<GenerationContext, Mapping> generate);
  }

  private Optional<Mapping> generateElement(GenerationContext ctx, Ast.MarkupElement node) {
    if (node.singletonTag().isPresent()) {
      return Optional.of(
        generateTag(
          ctx,
          random.element(Arrays.asList("singleton tag", "empty tag", "tag")),
          node.singletonTag().get()
        )
      );
    } else if (node.openingTag().isPresent() && node.closingTag().isPresent()) {
      // content gets messed up if there isn't a closing tag.
      List<Section> sections = new ArrayList<>(
        Arrays.asList(
          new NameSection(node.openingTag().get().name(), "tag"),
          new NodeSection<>(node.openingTag().get().attributeList())
            .setGenerate(this::generateFormattedText)
            .setDelete(a -> a.clear()),
          new NodeSection<>(node.contentList())
            .setGenerate(this::generateFormattedText)
            .setDelete(c -> c.clear()),
          new NodeSection<>(node.closingTag()).setImplicit(true)
        )
      );

      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          sections,
          n -> n.contentList().map(c -> c.rangeWithCommentsAndWhitespace().start),
          true
        )
      );
    } else if (
      !node.openingTag().isPresent() && !node.closingTag().isPresent() && node.text().isPresent()
    ) {
      // Non-tag text nodes are wrapped with MarkupElement also.
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          Arrays.asList(new NodeSection<>(node.text()).setGenerate(this::generateFormattedText))
        )
      );
    } else {
      return Optional.empty();
    }
  }

  private Mapping generateOpeningTag(GenerationContext ctx, Ast.MarkupOpeningTag node) {
    return generateTag(ctx, "open tag", node);
  }

  private Mapping generateClosingTag(GenerationContext ctx, Ast.MarkupClosingTag node) {
    return generateTag(ctx, "close tag", node);
  }

  private Mapping generateTag(GenerationContext ctx, String prefix, Ast.MarkupTag node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NameSection(node.name(), prefix),
        new NodeSection<>(node.attributeList())
          .setGenerate(this::generateFormattedText)
          .setDelete(a -> a.clear())
      )
    );
    return snippetGenerator.generate(
      ctx,
      node,
      sections,
      n ->
        n instanceof Ast.MarkupSingletonTag
          ? Optional.of(n.rangeWithCommentsAndWhitespace().stop - 1)
          : Optional.empty(),
      true
    );
  }

  private Mapping generateLambda(GenerationContext ctx, Ast.Lambda node) {
    String objectName = random
      .element(conversionMap.lambdaPrefixes())
      .stream()
      .collect(Collectors.joining(" "));

    // Python has special treatment for statements since it doesn't have a placeholder for a missing statement in lambdas.
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.modifierList())
          .setGenerate(this::generateFormattedText)
          .setDelete(p -> p.clear()),
        new PhraseSection(objectName),
        new NodeSection<>(node.parameter()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.parameterList())
          .setGenerate(this::generateFormattedText)
          .setDelete(p -> p.clear()),
        new NodeSection<>(node.typeOptional().flatMap(e -> e.optional()))
          .setGenerate(this::generateFormattedText)
          .setDelete(p -> p.remove()),
        language == Language.LANGUAGE_PYTHON
          ? new NodeSection<>(node.returnValue())
            .setGenerate(this::generateFormattedText)
            .setDelete(p -> p.remove())
          : new NodeSection<>(node.returnValue()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.functionModifierList()).setDelete(p -> p.clear()),
        new NodeSection<>(node.typeParameterList()).setDelete(p -> p.clear()),
        new NodeSection<>(node.returnValueNameListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.lambdaCaptureSpecifier()).setDelete(p -> p.remove()),
        language == Language.LANGUAGE_PYTHON
          ? new NodeSection<>(node.statement())
            .setGenerate(this::generateTerminatedElement)
            .setDelete(p -> p.remove())
          : new NodeSection<>(node.statement()).setGenerate(this::generateTerminatedElement),
        statementListSection(node.statementList())
      )
    );
    return snippetGenerator.generate(
      ctx,
      node,
      sections,
      n -> n.statementList().map(s -> s.rangeWithCommentsAndWhitespace().start)
    );
  }

  private Mapping generateFormattedText(GenerationContext ctx, AstNode node) {
    ctx = new GenerationContext(ctx);
    ctx.eligible = ctx.tokens.tokenRange(node.range());
    return generate.apply(ctx);
  }

  private NodeSection<Ast.StatementList> statementListSection(Ast.StatementList list) {
    return statementListSection(Optional.of(list));
  }

  private NodeSection<Ast.StatementList> statementListSection(Optional<Ast.StatementList> list) {
    return new NodeSection<>(list.filter(l -> l.elements().size() != 0))
      .setGenerate(
        (ctx, n) -> {
          Mapping ret = generateTerminatedElement(ctx, n.elements().get(0));
          ret.remaining.add(
            ctx.tokens.tokenRange(new Range(n.elements().get(0).range().stop, n.range().stop))
          );
          return ret;
        }
      )
      .setDelete(l -> l.clear());
  }

  private Mapping generateBegin(GenerationContext ctx, Ast.Begin node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new PhraseSection("begin"),
        statementListSection(node.clause().flatMap(c -> c.statementList()))
          .setDelete(s -> s.clear()),
        new NodeSection<>(node.rescueClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.elseClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.ensureClauseList()).setDelete(e -> e.clear())
      )
    );

    return snippetGenerator.generate(
      ctx,
      node,
      sections,
      n ->
        n
          .clause()
          .flatMap(c -> c.statementList().map(list -> list.rangeWithCommentsAndWhitespace().start))
    );
  }

  private Mapping generateComment(GenerationContext ctx, Ast.Comment node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new PhraseSection("comment"),
        new NodeSection<>(node.text()).setGenerate(this::generateFormattedText)
      )
    );
  }

  private Mapping generateClass(GenerationContext ctx, Ast.Class_ node) {
    String prefix = (language != Language.LANGUAGE_GO) ? "class" : "struct";
    List<Section> deletionSections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.memberList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.decoratorList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.implementsList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.typeParameterConstraintsOptional()).setDelete(e -> e.clear()),
        new NodeSection<>(node.rescueClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.elseClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.ensureClauseList()).setDelete(e -> e.clear())
      )
    );
    if (
      (language == Language.LANGUAGE_PYTHON) &&
      node.extendsList().isPresent() &&
      node.extendsList().get().code().equals("enum.Enum")
    ) {
      prefix = "enum";
    } else {
      deletionSections.add(new NodeSection<>(node.extendsList()).setDelete(e -> e.clear()));
      deletionSections.add(new NodeSection<>(node.extendsType()).setDelete(e -> e.remove()));
    }

    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NameSection(node.name(), prefix),
        new NodeSection<>(node.typeParameterList()).setGenerate(this::generateFormattedText)
      )
    );
    sections.addAll(deletionSections);
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Mapping generateCatch(GenerationContext ctx, Ast.CatchClause node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new PhraseSection(random.bool(0.5) ? "catch" : "except"),
        new NodeSection<>(node.parameter())
          .setImplicit(random.bool(0.1))
          .setGenerate(this::generateFormattedText),
        new NodeSection<>(node.statementList()).setDelete(l -> l.clear()),
        new NodeSection<>(node.statement()).setDelete(l -> l.remove()),
        new NodeSection<>(node.catchFilterOptional()).setDelete(o -> o.clear())
      ),
      n ->
        n
          .parameter()
          .map(p -> p.range().stop)
          .or(() -> n.statementList().map(list -> list.rangeWithCommentsAndWhitespace().start))
    );
  }

  private Mapping generateCssInclude(GenerationContext ctx, Ast.CssInclude node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new NameSection(node.name(), "include"),
        new NodeSection<>(node.argumentList()).setDelete(p -> p.clear()),
        new NodeSection<>(node.blockStatementList()).setDelete(p -> p.clear())
      )
    );
  }

  private Mapping generateCssMixin(GenerationContext ctx, Ast.CssMixin node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new NameSection(node.name(), "mixin"),
        new NodeSection<>(node.parameterList()).setDelete(p -> p.clear()),
        new NodeSection<>(node.blockStatementList()).setDelete(p -> p.clear())
      )
    );
  }

  private Mapping generateCssRuleset(GenerationContext ctx, Ast.CssRuleset node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.selectorList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.blockStatementList())
        .setDelete(
            p -> {
              p.clear();
            }
          )
      )
    );
    if (random.bool(0.5)) {
      sections.add(0, new PhraseSection("ruleset"));
    }
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Mapping generateDecorator(GenerationContext ctx, Ast.Decorator node) {
    String decoratorName = language.equals(Language.LANGUAGE_CSHARP)
      ? "attribute"
      : (random.bool(0.5) ? "decorator" : "annotation");

    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new PhraseSection(decoratorName),
        new NodeSection<>(node.value()).setGenerate(this::generateFormattedText)
      )
    );
  }

  private Optional<Mapping> generateIf(GenerationContext ctx, Ast.If node) {
    if (
      node.ifClause().isPresent() && node.ifClause().flatMap(c -> c.statementList()).isPresent()
    ) {
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          Arrays.asList(
            new PhraseSection("if"),
            new NodeSection<>(node.ifClause().flatMap(c -> c.modifierList()))
              .setGenerate(this::generateFormattedText)
              .setDelete(l -> l.clear()),
            new NodeSection<>(node.ifClause().flatMap(c -> c.blockInitializerOptional()))
              .setGenerate(this::generateFormattedText)
              .setDelete(l -> l.clear()),
            new NodeSection<>(node.ifClause().flatMap(c -> c.condition()))
              .setGenerate(this::generateFormattedText)
              .setDelete(this::deleteCondition),
            statementListSection(node.ifClause().flatMap(c -> c.statementList())),
            new NodeSection<>(node.elseIfClauseList()).setDelete(l -> l.clear()),
            new NodeSection<>(node.elseClauseOptional()).setDelete(l -> l.clear())
          ),
          n -> n.ifClause().flatMap(c -> c.condition().map(condition -> condition.range().stop))
        )
      );
    }
    return Optional.empty();
  }

  private Optional<Mapping> generateElseIf(GenerationContext ctx, Ast.ElseIfClause node) {
    if (node.statementList().isPresent()) {
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          Arrays.asList(
            new PhraseSection("else if"),
            new NodeSection<>(node.modifierList())
              .setGenerate(this::generateFormattedText)
              .setDelete(l -> l.clear()),
            new NodeSection<>(node.blockInitializerOptional())
              .setGenerate(this::generateFormattedText)
              .setDelete(l -> l.clear()),
            new NodeSection<>(node.condition())
              .setGenerate(this::generateFormattedText)
              .setDelete(this::deleteCondition),
            statementListSection(node.statementList())
          ),
          n -> n.condition().map(condition -> condition.range().stop)
        )
      );
    }
    return Optional.empty();
  }

  private Optional<Mapping> generateElse(GenerationContext ctx, Ast.ElseClause node) {
    if (node.statementList().isPresent()) {
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          Arrays.asList(new PhraseSection("else"), statementListSection(node.statementList())),
          n ->
            n.statement().isPresent()
              ? n.statement().map(s -> s.rangeWithCommentsAndWhitespace().start)
              : n.statementList().map(s -> s.rangeWithCommentsAndWhitespace().start)
        )
      );
    }
    return Optional.empty();
  }

  private Mapping generateEntry(GenerationContext ctx, Ast.KeyValuePair node) {
    AstNode key = node.key().get();
    if (
      key.children().size() == 1 && key.children().get(0) instanceof Ast.String_ && random.bool(0.9)
    ) {
      key = ((Ast.String_) key.children().get(0)).text();
    }

    List<Section> sections = new ArrayList<>();
    if (language == Language.LANGUAGE_JAVASCRIPT) {
      if (random.bool(0.5)) {
        sections.add(new PhraseSection("property"));
      } else {
        sections.add(new PhraseSection("entry"));
      }
    } else if (language == Language.LANGUAGE_SCSS) {
      if (random.bool(0.5)) {
        sections.add(new PhraseSection("property"));
      }
    } else {
      sections.add(new PhraseSection("entry"));
    }
    sections.addAll(
      Arrays.asList(
        new NodeSection<>(key).setGenerate(this::generateFormattedText),
        new PhraseSection(random.bool(0.5) ? "equals" : "colon"),
        new NodeSection<>(node.value()).setGenerate(this::generateFormattedText)
      )
    );

    return snippetGenerator.generate(ctx, node, sections);
  }

  private Mapping generateEnhancedFor(GenerationContext ctx, Ast.ForEachClause node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new PhraseSection(random.bool(0.75) ? "for" : "for each"),
        new NodeSection<>(node.modifierList()).setDelete(p -> p.clear()),
        new NodeSection<>(node.blockIterator()).setGenerate(this::generateFormattedText),
        new PhraseSection(node.of() ? "of" : "in"),
        new NodeSection<>(node.blockCollection()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.statement()).setDelete(p -> p.remove()),
        new NodeSection<>(node.statementList()).setDelete(p -> p.clear())
      )
    );
  }

  private Mapping generateEnsure(GenerationContext ctx, Ast.EnsureClause node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new PhraseSection("ensure"),
        statementListSection(node.statementList()).setDelete(s -> s.clear())
      ),
      n -> Optional.of(n.statementList().get().rangeWithCommentsAndWhitespace().start)
    );
  }

  private Mapping generateEnum(GenerationContext ctx, Ast.Enum node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NameSection(node.name(), "enum"),
        new NodeSection<>(node.typeParameterList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.extendsOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.extendsListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.implementsList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.memberList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.typeParameterConstraintsOptional()).setDelete(e -> e.clear()),
        new NodeSection<>(node.decoratorList()).setDelete(s -> s.clear())
      )
    );
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Mapping generateFinally(GenerationContext ctx, Ast.FinallyClause node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new PhraseSection("finally"),
        statementListSection(node.statementList()).setDelete(s -> s.clear())
      ),
      n ->
        n.statementList().map(statementList -> statementList.rangeWithCommentsAndWhitespace().start)
    );
  }

  private Optional<Mapping> generateFor(GenerationContext ctx, Ast.For node) {
    if (
      node.forEachClause().isPresent() &&
      node.forEachClause().flatMap(c -> c.statementList()).isPresent()
    ) {
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          Arrays.asList(
            new NodeSection<>(node.forEachClause()).setGenerate(this::generateEnhancedFor),
            new NodeSection<>(node.elseClauseOptional()).setDelete(s -> s.clear()),
            new NodeSection<>(node.loopLabelOptional()).setDelete(s -> s.clear())
          )
        )
      );
    }

    if (node.forClause().flatMap(c -> c.statementList()).isPresent()) {
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          Arrays.asList(new NodeSection<>(node.forClause()).setGenerate(this::generateStandardFor))
        )
      );
    }
    return Optional.empty();
  }

  private Mapping generateStandardFor(GenerationContext ctx, Ast.ForClause node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new PhraseSection("for"),
        new NodeSection<>(node.initializerOptional())
          .setGenerate(this::generateFormattedText)
          .setDelete(c -> c.clear()),
        new NodeSection<>(node.conditionOptional())
          .setGenerate(this::generateFormattedText)
          .setDelete(c -> c.clear()),
        new NodeSection<>(node.updateOptional())
          .setGenerate(this::generateFormattedText)
          .setDelete(c -> c.clear()),
        new NodeSection<>(node.statement()).setDelete(p -> p.remove()),
        new NodeSection<>(node.statementList()).setDelete(s -> s.clear())
      )
    );
    return snippetGenerator.generate(
      ctx,
      node,
      sections,
      n -> n.initializerOptional().map(o -> o.rangeWithCommentsAndWhitespace().start)
    );
  }

  private Optional<Mapping> generateFunction(GenerationContext ctx, Ast.Function node) {
    if (
      language == Language.LANGUAGE_CPLUSPLUS &&
      node.children().stream().filter(child -> child instanceof Ast.Identifier).count() > 1
    ) {
      return Optional.empty();
    }
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.typeOptional().flatMap(e -> e.optional()))
        .setGenerate(this::generateFormattedText),
        new NodeSection<>(node.modifierListSecond()).setGenerate(this::generateFormattedText),
        new NameSection(
          node.name(),
          language == Language.LANGUAGE_PYTHON && random.bool(0.05) ? "def" : "function"
        ),
        new NodeSection<>(node.typeParameterList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.receiverArgumentOptional()).setDelete(s -> s.clear()),
        language == Language.LANGUAGE_RUBY
          ? new NodeSection<>(node.parameterListOptional()).setDelete(s -> s.clear())
          : new NodeSection<>(node.parameterList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.namedParameterListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.positionalParameterListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.fieldInitializerList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.returnValueNameListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.statementList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.statement()).setDelete(s -> s.remove()),
        new NodeSection<>(node.decoratorList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.throwsType()).setDelete(s -> s.remove()),
        new NodeSection<>(node.typeParameterConstraintsOptional()).setDelete(e -> e.clear()),
        new NodeSection<>(node.functionModifierList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.rescueClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.elseClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.ensureClauseList()).setDelete(e -> e.clear())
      )
    );
    return Optional.of(snippetGenerator.generate(ctx, node, sections));
  }

  private Optional<Mapping> generateMethod(GenerationContext ctx, Ast.Method node) {
    if (node instanceof Ast.Constructor) {
      return Optional.empty();
    }
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.typeParameterList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.typeDecoratorList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.typeOptional().flatMap(e -> e.optional()))
        .setGenerate(this::generateFormattedText),
        new NodeSection<>(node.modifierListSecond()).setGenerate(this::generateFormattedText),
        new NameSection(node.name(), random.bool(0.05) ? "function" : "method"),
        new NodeSection<>(node.functionModifierList()).setGenerate(this::generateFormattedText),
        language == Language.LANGUAGE_RUBY
          ? new NodeSection<>(node.parameterListOptional()).setDelete(s -> s.clear())
          : new NodeSection<>(node.parameterList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.attributeList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.namedParameterListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.positionalParameterListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.fieldInitializerList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.returnValueNameListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.statementList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.statement()).setDelete(s -> s.remove()),
        new NodeSection<>(node.decoratorList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.throwsType()).setDelete(s -> s.remove()),
        new NodeSection<>(node.typeParameterConstraintsOptional()).setDelete(e -> e.clear()),
        new NodeSection<>(node.defaultMethodClause()).setDelete(e -> e.remove()),
        new NodeSection<>(node.deleteMethodClause()).setDelete(e -> e.remove()),
        new NodeSection<>(node.rescueClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.elseClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.ensureClauseList()).setDelete(e -> e.clear())
      )
    );

    return Optional.of(snippetGenerator.generate(ctx, node, sections));
  }

  private Mapping generateInterface(GenerationContext ctx, Ast.Interface node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NameSection(node.name(), "interface"),
        new NodeSection<>(node.typeParameterList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.memberList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.decoratorList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.implementsList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.extendsList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.extendsType()).setDelete(e -> e.remove()),
        new NodeSection<>(node.typeParameterConstraintsOptional()).setDelete(e -> e.clear())
      )
    );
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Mapping generateImplementation(GenerationContext ctx, Ast.Implementation node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.decoratorList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.typeParameterList()).setGenerate(this::generateFormattedText),
        new NameSection(node.name(), random.bool(0.5) ? "impl" : "implementation"),
        new NodeSection<>(node.implementsOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.memberList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.typeParameterConstraintsOptional()).setDelete(e -> e.clear())
      )
    );
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Optional<Mapping> generateLoop(GenerationContext ctx, Ast.Loop node) {
    if (node.statementList().isPresent()) {
      List<Section> sections = new ArrayList<>(
        Arrays.asList(
          new PhraseSection("loop"),
          statementListSection(node.statementList()),
          new NodeSection<>(node.loopLabelOptional()).setDelete(s -> s.clear())
        )
      );
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          sections,
          n -> n.statementList().map(list -> list.rangeWithCommentsAndWhitespace().start)
        )
      );
    }
    return Optional.empty();
  }

  private Mapping generateNamespace(GenerationContext ctx, Ast.Namespace node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NameSection(node.name(), language == Language.LANGUAGE_RUBY ? "module" : "namespace"),
        new NodeSection<>(node.statementList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.memberList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.rescueClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.elseClauseList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.ensureClauseList()).setDelete(e -> e.clear())
      )
    );
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Mapping generateParameter(GenerationContext ctx, Ast.Parameter node) {
    // object patterns in JavaScript don't have names, so just say the whole thing as formatted
    // text.
    // also just occasionally generate the whole thing as formatted text, eg. "a colon string" -->
    // a: str in python.
    if (
      !node.name().isPresent() ||
      !node.typeOptional().flatMap(t -> t.optional()).isPresent() ||
      random.bool(0.1)
    ) {
      return snippetGenerator.generate(
        ctx,
        node,
        Arrays.asList(
          new PhraseSection("parameter"),
          new NodeSection<>(node).setGenerate(this::generateFormattedText)
        )
      );
    }

    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.typeOptional().flatMap(t -> t.optional()))
        .setGenerate(this::generateFormattedText),
        new NodeSection<>(node.modifierListSecond())
          .setGenerate(this::generateFormattedText)
          .setDelete(s -> s.clear())
      )
    );
    sections.add(new NameSection(node.name(), "parameter"));
    if (node.value().isPresent()) {
      sections.add(new PhraseSection("equals"));
      sections.add(new NodeSection<>(node.value()).setGenerate(this::generateFormattedText));
    }
    sections.add(new NodeSection<>(node.decoratorList()).setDelete(s -> s.clear()));
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Optional<Mapping> generateProperty(GenerationContext ctx, Ast.Property node) {
    if (random.bool(0.05) && node.parent().map(p -> p instanceof Ast.Member).orElse(false)) {
      return Optional.empty(); // use default implicit generator from terminated element.
    }
    if (language == Language.LANGUAGE_GO) {
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          Arrays.asList(
            new NodeSection<>(node.typeOptional().flatMap(t -> t.optional()))
            .setGenerate(this::generateFormattedText),
            new NameSection(node.name(), propertyName()),
            new NodeSection<>(node.tagOptional().flatMap(t -> t.optional()))
            .setGenerate(this::generateFormattedText)
          )
        )
      );
    }

    // Only generate when we have a single assignment.
    boolean hasValidAssignmentList =
      node.assignmentList().isPresent() &&
      node.assignmentList().get().elements().size() == 1 &&
      node
        .assignmentList()
        .get()
        .elements()
        .get(0)
        .assignmentVariableList()
        .map(l -> l.elements().size() == 1)
        .orElse(false);

    if (hasValidAssignmentList && node.propertyAccessorListOptional().isEmpty()) {
      Ast.Assignment firstAssignment = (Ast.Assignment) node
        .assignmentList()
        .get()
        .elements()
        .get(0);
      Ast.AssignmentVariable firstAssignmentVariable = (Ast.AssignmentVariable) firstAssignment
        .assignmentVariableList()
        .map(c -> c.elements().get(0))
        .get();
      Optional<Ast.AssignmentValue> firstAssignmentValue = firstAssignment
        .assignmentValueListOptional()
        .flatMap(c -> c.assignmentValueList())
        .flatMap(list -> list.elements().stream().findFirst());

      List<Section> sections = new ArrayList<>(
        Arrays.asList(
          new NodeSection<>(node.modifierList())
            .setGenerate(this::generateFormattedText)
            .setDelete(e -> e.clear()),
          new NodeSection<>(node.typeOptional().flatMap(t -> t.optional()))
          .setGenerate(this::generateFormattedText),
          new NodeSection<>(node.decoratorList())
            .setGenerate(this::generateFormattedText)
            .setDelete(e -> e.clear())
        )
      );

      if (language == Language.LANGUAGE_CPLUSPLUS) {
        sections.add(
          new NodeSection<>(node.modifierListSecond()).setGenerate(this::generateFormattedText)
        );
        sections.add(new PhraseSection(propertyName()));
        sections.add(new NodeSection<>(firstAssignment).setGenerate(this::generateFormattedText));
      } else {
        sections.addAll(
          Arrays.asList(
            new NodeSection<>(firstAssignment.typeOptional().flatMap(t -> t.optional()))
            .setGenerate(this::generateFormattedText),
            new NodeSection<>(firstAssignmentVariable.typeOptional().flatMap(t -> t.optional()))
            .setGenerate(this::generateFormattedText),
            new NameSection(firstAssignmentVariable.name(), propertyName())
          )
        );
        if (firstAssignmentValue.isPresent()) {
          sections.add(new PhraseSection("equals"));
          sections.add(
            new NodeSection<>(firstAssignmentValue.get())
              .setGenerate(this::generateFormattedText)
              .setDelete(e -> e.remove())
          );
        }
      }
      return Optional.of(snippetGenerator.generate(ctx, node, sections));
    }

    return Optional.empty();
  }

  private Optional<Mapping> generatePrototype(GenerationContext ctx, Ast.Prototype node) {
    if (
      language == Language.LANGUAGE_CPLUSPLUS &&
      node.children().stream().filter(child -> child instanceof Ast.Identifier).count() > 1
    ) {
      return Optional.empty();
    }
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.typeOptional().flatMap(e -> e.optional()))
        .setGenerate(this::generateFormattedText),
        new NodeSection<>(node.modifierListSecond()).setGenerate(this::generateFormattedText),
        new NameSection(node.name(), random.bool(0.5) ? "prototype" : "function prototype"),
        new NodeSection<>(node.typeParameterList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.attributeList()).setDelete(e -> e.clear()),
        new NodeSection<>(node.receiverArgumentOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.parameterList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.namedParameterListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.positionalParameterListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.fieldInitializerList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.returnValueNameListOptional()).setDelete(s -> s.clear()),
        new NodeSection<>(node.statementList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.statement()).setDelete(s -> s.remove()),
        new NodeSection<>(node.decoratorList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.throwsType()).setDelete(s -> s.remove()),
        new NodeSection<>(node.typeParameterConstraintsOptional()).setDelete(e -> e.clear()),
        new NodeSection<>(node.functionModifierList()).setDelete(s -> s.clear())
      )
    );
    return Optional.of(snippetGenerator.generate(ctx, node, sections));
  }

  private Mapping generateType(GenerationContext ctx, Ast.Type node) {
    if (node.parent().map(parent -> parent instanceof Ast.ReturnTypeOptional).orElse(false)) {
      return snippetGenerator.generate(
        ctx,
        node,
        Arrays.asList(
          new PhraseSection(random.bool(0.5) ? "return type" : "type"),
          new NodeSection<>(node).setGenerate(this::generateFormattedText)
        )
      );
    } else {
      return snippetGenerator.generate(
        ctx,
        node,
        Arrays.asList(
          new PhraseSection("type"),
          new NodeSection<>(node).setGenerate(this::generateFormattedText)
        )
      );
    }
  }

  private Mapping generateRescue(GenerationContext ctx, Ast.RescueClause node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new PhraseSection("rescue"),
        new NodeSection<>(node.parameter())
          .setImplicit(random.bool(0.1))
          .setGenerate(this::generateFormattedText),
        new NodeSection<>(node.statementList()).setDelete(s -> s.clear())
      ),
      n ->
        Optional.of(
          n
            .parameter()
            .map(p -> p.range().stop)
            .orElse(n.statementList().get().rangeWithCommentsAndWhitespace().start)
        )
    );
  }

  private Mapping generateTrait(GenerationContext ctx, Ast.Trait node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.decoratorList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.typeParameterList()).setGenerate(this::generateFormattedText),
        new NameSection(node.name(), "trait"),
        new NodeSection<>(node.implementsList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.traitMemberList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.typeParameterConstraintsOptional()).setDelete(e -> e.clear()),
        new NodeSection<>(node.child(Ast.TraitBoundsOptional.class)).setDelete(e -> e.remove())
      )
    );
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Optional<Mapping> generateTry(GenerationContext ctx, Ast.Try node) {
    if (node.clause().isPresent() && node.clause().flatMap(c -> c.statementList()).isPresent()) {
      List<Section> sections = new ArrayList<>(
        Arrays.asList(
          new PhraseSection("try"),
          statementListSection(node.clause().flatMap(c -> c.statementList()))
            .setDelete(s -> s.clear()),
          new NodeSection<>(node.elseClauseOptional()).setDelete(e -> e.clear())
        )
      );

      if (node.catchClauseList().map(c -> c.elements().size() > 0).orElse(false)) {
        // Only generate the first catch implicitly.
        if (node.catchClauseList().map(c -> c.elements().size() > 1).orElse(false)) {
          for (int i = 1; i < node.catchClauseList().get().elements().size(); i++) {
            sections.add(
              new NodeSection<>(node.catchClauseList().get().elements().get(i))
              .setDelete(e -> e.remove())
            );
          }
        }

        Ast.CatchClause firstCatch = node.catchClauseList().get().elements().get(0);
        sections.addAll(
          Arrays.asList(
            new NodeSection<>(firstCatch.parameter()).setImplicit(true),
            new NodeSection<>(firstCatch.statementList()).setDelete(l -> l.clear()),
            new NodeSection<>(firstCatch.statement()).setDelete(l -> l.remove())
          )
        );

        sections.add(new NodeSection<>(node.finallyOptional()).setDelete(o -> o.clear()));
      } else {
        // only a finally exists, clear the statment list.
        sections.add(
          new NodeSection<>(node.finallyOptional().flatMap(c -> c.optional()).get().statementList())
            .setDelete(l -> l.clear())
            .setImplicit(true)
        );
      }
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          sections,
          n ->
            n
              .clause()
              .flatMap(
                c -> c.statementList().map(list -> list.rangeWithCommentsAndWhitespace().start)
              )
        )
      );
    }
    return Optional.empty();
  }

  private Optional<Mapping> generateWrapperElement(GenerationContext ctx, AstNode node) {
    if (node.children().size() > 0) {
      AstNode firstChild = node.children().get(0);
      Optional<Class<? extends AstNode>> instance = _snippets
        .keySet()
        .stream()
        .filter(type -> type.isInstance(firstChild))
        .findFirst();

      return instance.map(
        i ->
          snippetGenerator.generate(
            ctx,
            node,
            Arrays.asList(
              new NodeSection<>(instance.get().cast(firstChild))
              .setGenerate(
                  (innerCtx, innerNode) ->
                    _snippets
                      .get(i)
                      .apply(innerCtx, innerNode)
                      .orElse(generateFormattedText(innerCtx, innerNode))
                )
            )
          )
      );
    }
    return Optional.empty();
  }

  private Mapping generateTerminatedElement(GenerationContext ctx, Ast.TerminatedElement node) {
    List<Section> sections = new ArrayList<>();
    if (node.inner().isPresent()) {
      sections.add(
        new NodeSection<>(node.inner().get())
        .setGenerate(
            (innerCtx, innerNode) ->
              _snippets
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().isInstance(innerNode))
                .flatMap(entry -> entry.getValue().apply(innerCtx, innerNode).stream())
                .findFirst()
                .orElseGet(() -> generateFormattedText(innerCtx, innerNode))
          )
      );
    } else {
      sections.add(new NodeSection<>(node).setGenerate(this::generateFormattedText));
    }
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Mapping generateStruct(GenerationContext ctx, Ast.Struct node) {
    List<Section> sections = new ArrayList<>(
      Arrays.asList(
        new NodeSection<>(node.decoratorList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.typeParameterList()).setGenerate(this::generateFormattedText),
        new NameSection(node.name(), "struct"),
        new NodeSection<>(node.implementsList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.extendsList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.memberList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.anonymousPropertyList()).setDelete(s -> s.clear()),
        new NodeSection<>(node.typeParameterConstraintsOptional()).setDelete(e -> e.clear()),
        new NodeSection<>(node.child(Ast.TemplateArgumentList.class)).setDelete(e -> e.remove())
      )
    );
    return snippetGenerator.generate(ctx, node, sections);
  }

  private Mapping generateUntil(GenerationContext ctx, Ast.Until node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new PhraseSection("until"),
        new NodeSection<>(node.clause().get().condition())
          .setGenerate(this::generateFormattedText)
          .setDelete(this::deleteCondition),
        new NodeSection<>(node.clause().get().statementList()).setDelete(s -> s.clear())
      ),
      n -> Optional.of(n.clause().get().condition().get().rangeWithCommentsAndWhitespace().stop)
    );
  }

  private Optional<Mapping> generateWhile(GenerationContext ctx, Ast.While node) {
    if (
      node.whileClause().isPresent() &&
      node.whileClause().flatMap(c -> c.statementList()).isPresent()
    ) {
      return Optional.of(
        snippetGenerator.generate(
          ctx,
          node,
          Arrays.asList(
            new PhraseSection("while"),
            new NodeSection<>(node.whileClause().flatMap(c -> c.condition()))
              .setGenerate(this::generateFormattedText)
              .setDelete(this::deleteCondition),
            new NodeSection<>(node.whileClause().flatMap(c -> c.statementList()))
            .setDelete(s -> s.clear()),
            new NodeSection<>(node.elseClauseOptional()).setDelete(s -> s.clear()),
            new NodeSection<>(node.loopLabelOptional()).setDelete(s -> s.clear())
          ),
          n -> n.whileClause().flatMap(c -> c.condition().map(condition -> condition.range().stop))
        )
      );
    }
    return Optional.empty();
  }

  private Mapping generateWith(GenerationContext ctx, Ast.With node) {
    return snippetGenerator.generate(
      ctx,
      node,
      Arrays.asList(
        new NodeSection<>(node.modifierList()).setGenerate(this::generateFormattedText),
        new PhraseSection("with"),
        new NodeSection<>(node.itemList()).setGenerate(this::generateFormattedText),
        new NodeSection<>(node.statement()).setDelete(p -> p.remove()),
        new NodeSection<>(node.statementList()).setDelete(p -> p.clear())
      )
    );
  }

  private void deleteCondition(Ast.Condition conditionNode) {
    if (
      conditionNode
        .rightMostLeft(AstToken.class)
        .map(token -> conditionNode.tree().whitespace.isWhitespace(token))
        .orElse(false)
    ) {
      conditionNode.replace(
        astFactory.create(
          new Ast.Condition(),
          language == Language.LANGUAGE_PYTHON ? "True" : "true"
        )
      );
    } else {
      conditionNode.replace(
        astFactory.create(
          new Ast.Condition(),
          language == Language.LANGUAGE_PYTHON ? " True" : " true"
        )
      );
    }
  }

  private String propertyName() {
    return random.element(Arrays.asList("property", "field", "member"));
  }

  private <T extends AstNode> void registerOptional(
    Class<T> type,
    BiFunction<GenerationContext, T, Optional<Mapping>> generate
  ) {
    _snippets.put(
      type,
      (ctx, node) -> {
        try {
          return generate.apply(ctx, type.cast(node));
        } catch (SnippetGenerationException error) {
          // Error nodes in the parse sometimes cause errors during snippet generation, so we ignore them and throw out the snippet.
          if (node.tree().syntaxError.isPresent()) {
            return Optional.<Mapping>empty();
          }
          throw error;
        }
      }
    );
  }

  private <T extends AstNode> void register(
    Class<T> type,
    BiFunction<GenerationContext, T, Mapping> generate
  ) {
    registerOptional(type, (ctx, node) -> Optional.of(generate.apply(ctx, node)));
  }

  private <T extends AstNode> void register(Class<T> type, String prefix) {
    register(type, Arrays.asList(prefix));
  }

  private <T extends AstNode> void register(Class<T> type, List<String> prefixes) {
    register(
      type,
      (ctx, node) ->
        snippetGenerator.generate(
          ctx,
          node,
          Arrays.asList(
            new PhraseSection(random.element(prefixes)),
            new NodeSection<>(node).setGenerate(this::generateFormattedText)
          )
        )
    );
  }

  public Optional<Mapping> generateSnippet(GenerationContext ctx, AstNode node) {
    return map()
      .entrySet()
      .stream()
      .filter(e -> e.getKey().isInstance(node))
      .map(e -> e.getValue())
      .findFirst()
      .orElse(
        (innerCtx, innerNode) -> {
          // If we can't find anything, then sample implicitly for sake of bare add.
          if (node.parent().map(p -> p instanceof AstList).orElse(false) && random.bool(0.05)) {
            if (innerNode instanceof Ast.TerminatedElement) {
              return Optional.of(
                generateTerminatedElement(innerCtx, (Ast.TerminatedElement) innerNode)
              );
            } else {
              return Optional.of(generateFormattedText(innerCtx, innerNode));
            }
          }
          return Optional.empty();
        }
      )
      .apply(ctx, node);
  }

  public Map<Class<? extends AstNode>, BiFunction<GenerationContext, AstNode, Optional<Mapping>>> map() {
    if (_snippets == null) {
      _snippets = new HashMap<>();

      registerOptional(Ast.RescueElseEnsureElement.class, this::generateWrapperElement);
      registerOptional(Ast.MarkupContent.class, this::generateWrapperElement);
      register(Ast.Member.class, this::generateTerminatedElement);
      register(Ast.Statement.class, this::generateTerminatedElement);

      register(Ast.Begin.class, this::generateBegin);
      register(Ast.Class_.class, this::generateClass);
      register(Ast.CssInclude.class, this::generateCssInclude);
      register(Ast.CssMixin.class, this::generateCssMixin);
      register(Ast.CssRuleset.class, this::generateCssRuleset);
      register(Ast.Enum.class, this::generateEnum);
      registerOptional(Ast.For.class, this::generateFor);
      registerOptional(Ast.Function.class, this::generateFunction);
      registerOptional(Ast.If.class, this::generateIf);
      register(Ast.Implementation.class, this::generateImplementation);
      register(Ast.Import.class, this::generateTerminatedElement);
      register(Ast.Interface.class, this::generateInterface);
      register(Ast.KeyValuePair.class, this::generateEntry);
      registerOptional(Ast.Loop.class, this::generateLoop);
      registerOptional(Ast.Method.class, this::generateMethod);
      register(Ast.Namespace.class, this::generateNamespace);
      registerOptional(Ast.Property.class, this::generateProperty);
      registerOptional(Ast.Prototype.class, this::generatePrototype);
      register(Ast.Struct.class, this::generateStruct);
      register(Ast.Throw.class, this::generateFormattedText);
      register(Ast.Trait.class, this::generateTrait);
      registerOptional(Ast.Try.class, this::generateTry);
      register(Ast.With.class, this::generateWith);
      registerOptional(Ast.While.class, this::generateWhile);

      registerOptional(Ast.MarkupElement.class, this::generateElement);
      register(Ast.MarkupText.class, this::generateFormattedText);
      register(Ast.MarkupOpeningTag.class, this::generateOpeningTag);
      register(Ast.MarkupClosingTag.class, this::generateClosingTag);

      register(Ast.CatchClause.class, this::generateCatch);
      register(Ast.Comment.class, this::generateComment);
      register(Ast.Decorator.class, this::generateDecorator);
      registerOptional(Ast.ElseClause.class, this::generateElse);
      registerOptional(Ast.ElseIfClause.class, this::generateElseIf);
      register(Ast.EnsureClause.class, this::generateEnsure);
      register(Ast.FinallyClause.class, this::generateFinally);
      register(Ast.Until.class, this::generateUntil);
      register(Ast.KeyValuePair.class, this::generateEntry);
      register(Ast.Lambda.class, this::generateLambda);
      register(Ast.Parameter.class, this::generateParameter);
      register(Ast.RescueClause.class, this::generateRescue);
      register(Ast.Type.class, this::generateType);
      register(Ast.Until.class, this::generateUntil);

      register(Ast.Argument.class, "argument");

      // Some languages have a combined notion of implements and extends (c#).
      register(
        Ast.ExtendsType.class,
        Arrays.asList("parent", "extends", "superclass", "implements")
      );
      register(Ast.ImplementsType.class, "implements");
      register(Ast.ListElement.class, "element");
      register(Ast.MarkupAttribute.class, "attribute");
      register(Ast.Modifier.class, "modifier");
      register(Ast.ReturnValue.class, "return value");

      if (languageDeterminer.languagesWithMlSnippetsDisabled().contains(language)) {
        _snippets.clear();
      }
      if (languageSpecificDisabledSnippets.containsKey(language)) {
        languageSpecificDisabledSnippets.get(language).stream().forEach(v -> _snippets.remove(v));
      }
      disabledSnippets.stream().forEach(e -> _snippets.remove(e));
    }
    return _snippets;
  }
}
