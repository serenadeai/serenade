package corpusgen.mapping;

import core.ast.Ast;
import core.ast.api.AstContainer;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.codeengine.AlphaNumericFinder;
import core.codeengine.AlphaStyle;
import core.codeengine.InputConverter;
import core.codeengine.Tokenizer;
import core.gen.rpc.Language;
import core.util.Range;
import corpusgen.util.Symbols;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import toolbelt.languages.LanguageDeterminer;

public class FullContextMappingGenerator {

  AlphaNumericFinder alphaNumericFinder;
  CompositeWords compositeWords;
  CorpusGenAstFactory corpusGenAstFactory;
  InputConverter inputConverter;
  LanguageDeterminer languageDeterminer;
  FullContextMapping.Factory mappingWithContextFactory;
  MappingGenerator mappingGenerator;
  Tokenizer tokenizer;
  Sampler.Factory samplerFactory;
  Symbols symbols;

  Config config;

  @AssistedInject
  public FullContextMappingGenerator(
    AlphaNumericFinder alphaNumericFinder,
    CompositeWords compositeWords,
    CorpusGenAstFactory corpusGenAstFactory,
    InputConverter inputConverter,
    LanguageDeterminer languageDeterminer,
    FullContextMapping.Factory mappingWithContextFactory,
    MappingGenerator.Factory mappingGeneratorFactory,
    Tokenizer tokenizer,
    Sampler.Factory samplerFactory,
    Symbols symbols,
    @Assisted Config config
  ) {
    this.alphaNumericFinder = alphaNumericFinder;
    this.compositeWords = compositeWords;
    this.corpusGenAstFactory = corpusGenAstFactory;
    this.inputConverter = inputConverter;
    this.languageDeterminer = languageDeterminer;
    this.mappingWithContextFactory = mappingWithContextFactory;
    this.mappingGenerator = mappingGeneratorFactory.create(config);
    this.tokenizer = tokenizer;
    this.samplerFactory = samplerFactory;
    this.symbols = symbols;
    this.config = config;
  }

  @AssistedFactory
  public interface Factory {
    FullContextMappingGenerator create(Config config);
  }

  public List<FullContextMapping> generateMappings(
    String source,
    Optional<Set<Class<? extends AstNode>>> include
  ) {
    source = source.replaceAll("\t", "    ");
    source = symbols.stripNonAscii(source);
    Tokens tokens = new Tokens(tokenizer.tokenize(source));
    // Having an AST is optional for generateInsertMappings, but not generateAddMappings
    Optional<AstObjects> astObjects;
    if (config.language.equals(Language.LANGUAGE_DEFAULT) || languageDeterminer.languagesWithMlSnippetsDisabled().contains(config.language)) {
      astObjects = Optional.empty();
    } else {
      AstParent root = corpusGenAstFactory.createFileRoot(source, config.language);
      astObjects = Optional.of(new AstObjects(root));
    }
    List<FullContextMapping> mappings = new ArrayList<FullContextMapping>();
    if (include.isEmpty()) {
      mappings.addAll(generateInsertMappings(tokens, astObjects));
    }
    if (config.includeAddMappings && astObjects.isPresent()) {
      mappings.addAll(generateAddMappings(tokens, astObjects.get(), include));
    }
    // Don't output mappings with empty inputs.
    mappings =
      mappings.stream().filter(m -> m.mapping.phrases.size() != 0).collect(Collectors.toList());
    return mappings;
  }

  private List<Tokenizer.Token> splitPlural(List<Tokenizer.Token> tokens, int index) {
    tokens = new ArrayList<>(tokens);
    Tokenizer.AlphaToken pluralToken = (Tokenizer.AlphaToken) tokens.get(index);
    String singularWord = compositeWords.removePluralization(pluralToken.word());
    Tokenizer.AlphaToken singularToken = new Tokenizer.AlphaToken(
      pluralToken.source,
      new Range(pluralToken.range.start, pluralToken.range.start + singularWord.length()),
      pluralToken.style
    );
    Tokenizer.AlphaToken postfixToken = new Tokenizer.AlphaToken(
      pluralToken.source,
      new Range(
        pluralToken.range.start + singularWord.length(),
        pluralToken.range.start + pluralToken.word().length()
      ),
      pluralToken.style == AlphaStyle.CAPS ? AlphaStyle.CAPS : AlphaStyle.LOWERCASE
    );
    tokens.set(index, singularToken);
    tokens.add(index + 1, postfixToken);
    return tokens;
  }

  private List<FullContextMapping> generateInsertMappings(
    Tokens tokens,
    Optional<AstObjects> astObjects
  ) {
    List<FullContextMapping> mappings = new ArrayList<FullContextMapping>();
    List<Range> allRemaining = new ArrayList<>(Arrays.asList(new Range(0, tokens.list.size())));
    while (allRemaining.size() > 0) {
      Range eligible = allRemaining.remove(0);
      Sampler sampler = samplerFactory.create(config);
      Optional<Tokens> adjustedTokens = Optional.empty();

      // A lot of times the speech engine misses plurals, so people need to say "insert s" or
      // "insert es"
      // at the end of the word. This samples splitting the postfix off of the previous word.
      if (
        eligible.start > 0 && tokens.list.get(eligible.start - 1) instanceof Tokenizer.AlphaToken
      ) {
        Tokenizer.AlphaToken pluralToken = (Tokenizer.AlphaToken) tokens.list.get(
          eligible.start - 1
        );
        if (sampler.splitPlural() && compositeWords.isPlural(pluralToken.word())) {
          eligible = new Range(eligible.start, eligible.stop + 1);
          adjustedTokens = Optional.of(new Tokens(splitPlural(tokens.list, eligible.start - 1)));
        }
      }

      Mapping m = mappingGenerator.generate(
        new GenerationContext(
          astObjects,
          adjustedTokens.orElse(tokens),
          sampler,
          eligible,
          /* partial */true,
          /* trailing implicit symbols */false
        )
      );

      if (m.outputTokens.size() != 0) {
        mappings.add(
          mappingWithContextFactory.create(
            m,
            adjustedTokens.orElse(tokens),
            (
              (Tokenizer.CodeToken) adjustedTokens.orElse(tokens).list.get(eligible.start)
            ).range.start,
            Optional.empty()
          )
        );
      }

      // Revert indexing so that it uses the original token list.
      int pluralOffset = adjustedTokens.isPresent() ? 1 : 0;
      allRemaining.addAll(
        0,
        m.remaining
          .stream()
          .map(r -> new Range(r.start - pluralOffset, r.stop - pluralOffset))
          .collect(Collectors.toList())
      );
    }
    return mappings;
  }

  private List<FullContextMapping> generateAddMappings(
    Tokens tokens,
    AstObjects astObjects,
    Optional<Set<Class<? extends AstNode>>> include
  ) {
    return Stream
      .concat(astObjects.root.tree().comments.stream(), astObjects.root.find(AstNode.class))
      .filter(n -> n.parent().map(p -> p instanceof AstContainer).orElse(false))
      .flatMap(
        node ->
          mappingGenerator
            .generateSnippets(
              new GenerationContext(
                Optional.of(astObjects),
                tokens,
                samplerFactory.create(config),
                node.range(),
                /* partial */true,
                /* trailing implicit symbols */false
              ),
              node,
              include
            )
            .map(
              m ->
                mappingWithContextFactory.create(m, tokens, node.range().start, Optional.of(node))
            )
            .stream()
      )
      .collect(Collectors.toList());
  }
}
