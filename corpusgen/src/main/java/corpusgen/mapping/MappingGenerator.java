package corpusgen.mapping;

import core.ast.api.AstList;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.codeengine.AlphaNumericFinder;
import core.codeengine.AlphaStyle;
import core.codeengine.Tokenizer;
import core.exception.SnippetGenerationException;
import core.formattedtext.ConversionMap;
import core.formattedtext.ConversionMapFactory;
import core.gen.rpc.Language;
import core.util.FillerWords;
import core.util.Range;
import core.util.TextStyle;
import core.util.TextStyler;
import corpusgen.util.NumberGenerator;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MappingGenerator {

  AlphaNumericFinder alphaNumericFinder;
  CompositeWords compositeWords;
  NumberGenerator numberGenerator;
  TextStyler textStyler = new TextStyler();
  AlternativeWordSampler alternativeWordSampler;
  FillerWords fillerWords;
  Random random;
  Snippets snippets;

  Config config;

  private ConversionMap conversionMap;
  private Map<String, String> enclosureStartToEnd;
  private Map<String, List<String>> enclosureStartToNames;

  private Map<TextStyle, List<String>> styleToNames;
  private Map<String, List<String>> symbolToPhrases;
  private Map<String, List<String>> symbolToPrioritizedPhrases;
  private Map<String, List<String>> aliases;

  @AssistedInject
  public MappingGenerator(
    AlphaNumericFinder alphaNumericFinder,
    CompositeWords compositeWords,
    NumberGenerator numberGenerator,
    ConversionMapFactory conversionMapFactory,
    AlternativeWordSampler alternativeWordSampler,
    FillerWords fillerWords,
    Random random,
    Snippets.Factory snippetsFactory,
    @Assisted Config config
  ) {
    this.alphaNumericFinder = alphaNumericFinder;
    this.compositeWords = compositeWords;
    this.conversionMap = conversionMapFactory.create(config.language);
    this.numberGenerator = numberGenerator;
    this.alternativeWordSampler = alternativeWordSampler;
    this.fillerWords = fillerWords;
    this.random = random;
    this.snippets = snippetsFactory.create(config.language, this::generate);
    this.config = config;

    enclosureStartToEnd = new HashMap<String, String>();
    enclosureStartToNames = new HashMap<String, List<String>>();
    for (String name : conversionMap.enclosureNameToStart.keySet()) {
      String start = conversionMap.enclosureNameToStart.get(name);
      String end = conversionMap.enclosureNameToStop.get(name);
      enclosureStartToEnd.put(start, end);
      if (!enclosureStartToNames.containsKey(start)) {
        enclosureStartToNames.put(start, new ArrayList<>());
      }
      enclosureStartToNames.get(start).add(name);
    }
    // Enable saying things like foo equals string hello -> foo = "hello".
    enclosureStartToNames.get("'").add("string");
    enclosureStartToNames.get("\"").add("string");

    symbolToPhrases = new HashMap<String, List<String>>();
    for (Map.Entry<List<String>, String> entry : conversionMap.symbolMap.entrySet()) {
      String replacement = entry.getKey().stream().collect(Collectors.joining(" "));
      String symbol = entry.getValue();
      if (!symbolToPhrases.containsKey(symbol)) {
        symbolToPhrases.put(symbol, new ArrayList<>());
      }
      symbolToPhrases.get(symbol).add(replacement);
    }
    if (config.language.equals(Language.LANGUAGE_CPLUSPLUS)) {
      symbolToPhrases.put("&", Arrays.asList("address of", "ampersand"));
    }
    symbolToPhrases.get("==").addAll(Arrays.asList("equal", "equals"));
    symbolToPhrases.put("std", Arrays.asList("standard"));
    symbolToPhrases.put("=>", Arrays.asList("arrow", "right arrow"));
    symbolToPhrases.put("->", Arrays.asList("arrow", "right arrow"));
    symbolToPhrases.put("**", Arrays.asList("exponent"));

    styleToNames = new HashMap<TextStyle, List<String>>();
    for (Map.Entry<List<String>, TextStyle> entry : conversionMap.styleMap.entrySet()) {
      if (!styleToNames.containsKey(entry.getValue())) {
        styleToNames.put(entry.getValue(), new ArrayList<>());
      }
      styleToNames.get(entry.getValue()).add(String.join(" ", entry.getKey()));
    }

    symbolToPrioritizedPhrases = new HashMap<String, List<String>>();
    symbolToPrioritizedPhrases.put("(", Arrays.asList("left paren", "paren", "open paren"));
    symbolToPrioritizedPhrases.put(")", Arrays.asList("right paren", "close paren"));
    symbolToPrioritizedPhrases.put(".", Arrays.asList("dot"));
    if (config.language == Language.LANGUAGE_DEFAULT) {
      symbolToPrioritizedPhrases.put(".", Arrays.asList("period"));
    } else {
      symbolToPrioritizedPhrases.put(".", Arrays.asList("dot"));
    }

    List<List<String>> aliasGroups = Arrays.asList(
      Arrays.asList("string", "str"),
      Arrays.asList("boolean", "bool"),
      Arrays.asList("integer", "int"),
      Arrays.asList("standard", "std"),
      Arrays.asList("standard io", "stdio"),
      Arrays.asList("standard in", "stdin")
    );
    aliases = new HashMap<String, List<String>>();
    for (List<String> aliasGroup : aliasGroups) {
      for (String alias : aliasGroup) {
        aliases.put(alias, new ArrayList<>(aliasGroup));
        aliases.get(alias).remove(alias);
      }
    }
  }

  @AssistedFactory
  public interface Factory {
    MappingGenerator create(Config config);
  }

  private String sampleAliases(String word) {
    if (aliases.containsKey(word) && random.bool(0.1)) {
      word = random.element(aliases.get(word));
    }
    return word;
  }

  private List<String> sampleEnglish(Tokenizer.NumberToken token) {
    List<String> phrases = new ArrayList<>();
    if (random.bool(0.001)) {
      phrases.addAll(random.element(conversionMap.numeralPrefixes));
    }
    phrases.add(numberGenerator.sampleEnglish(token.originalCode()));
    return phrases;
  }

  private Mapping continueGeneration(
    GenerationContext ctx,
    List<String> phrasesPrefix,
    List<Tokenizer.Token> outputTokensPrefix,
    int numConsumed
  ) {
    Mapping ret = continueGeneration(ctx, numConsumed);
    ret.outputTokens.addAll(0, outputTokensPrefix);
    ret.phrases.addAll(0, phrasesPrefix);
    return ret;
  }

  private Mapping continueGeneration(GenerationContext ctx, int numConsumed) {
    Range nextEligible = new Range(ctx.eligible.start + numConsumed, ctx.eligible.stop);
    return continueGeneration(ctx, nextEligible);
  }

  private Mapping continueGeneration(GenerationContext ctx, Range nextEligible) {
    if (nextEligible.start == nextEligible.stop) {
      return new Mapping(Collections.emptyList(), false);
    }
    if (ctx.allowPartial && ctx.sampler.stop()) {
      return new Mapping(Arrays.asList(nextEligible), true);
    }
    ctx = new GenerationContext(ctx);
    ctx.eligible = nextEligible;
    Mapping ret = generate(ctx);

    // If next thing an explicit ; or a space, then drop it most of the time.
    if (
      ret.outputTokens.size() == 1 &&
      random.bool(0.9) &&
      ret.phrases.size() > 0 &&
      (
        ret.outputTokens.get(0).originalCode().equals(";") ||
        ret.outputTokens.get(0) instanceof Tokenizer.SpaceToken
      )
    ) {
      return new Mapping(Collections.emptyList(), Collections.emptyList(), ret.remaining, false);
    }
    return ret;
  }

  private Mapping generateSpaces(GenerationContext ctx) {
    // Aggregate adjacent spaces.
    int index = ctx.eligible.start;
    List<Tokenizer.Token> spaceTokens = new ArrayList<>();
    while (
      index < ctx.tokens.list.size() && ctx.tokens.list.get(index) instanceof Tokenizer.SpaceToken
    ) {
      spaceTokens.add(ctx.tokens.list.get(index));
      index++;
    }

    // Don't usually say spaces. When we're saying spaces, say them 2/3 of the time.
    // Make sure we're allowed to have trailing trailing spaces (like if we're inside an enclosure).
    List<String> phrases = new ArrayList<String>();
    Mapping result = continueGeneration(ctx, spaceTokens.size());
    if (
      (result.outputTokens.size() == 0 && !ctx.allowTrailingImplicit) ||
      ctx.sampler.explicitSpaces(spaceTokens.size() > 1)
    ) {
      for (Tokenizer.Token spaceToken : spaceTokens) {
        phrases.add("space");
      }
    }
    result.phrases.addAll(0, phrases);
    result.outputTokens.addAll(0, spaceTokens);
    return result;
  }

  public Mapping generate(GenerationContext ctx) {
    if (ctx.eligible.start == ctx.eligible.stop) {
      return new Mapping(Collections.emptyList(), false);
    }
    if (config.sampleFillerWords && random.bool(0.005)) {
      return continueGeneration(
        ctx,
        Arrays.asList(fillerWords.sample()),
        Collections.emptyList(),
        0
      );
    }

    Tokenizer.Token token = ctx.tokens.list.get(ctx.eligible.start);
    Optional<Mapping> snippetMapping = generateSnippet(ctx);
    if (snippetMapping.isPresent()) {
      return snippetMapping.get();
    }
    if (token instanceof Tokenizer.SpaceToken) {
      return generateSpaces(ctx);
    } else if (token instanceof Tokenizer.NumberToken) {
      return continueGeneration(
        ctx,
        sampleEnglish((Tokenizer.NumberToken) token),
        Arrays.asList(token),
        1
      );
    } else if (token instanceof Tokenizer.SymbolToken) {
      return generateEnclosure(ctx).orElseGet(() -> generateSymbol(ctx).get());
    } else if (
      token instanceof Tokenizer.NewlineToken ||
      token instanceof Tokenizer.IndentToken ||
      token instanceof Tokenizer.DedentToken
    ) {
      List<Range> remaining = new ArrayList<>();
      if (ctx.eligible.start + 1 != ctx.eligible.stop) {
        remaining.add(new Range(ctx.eligible.start + 1, ctx.eligible.stop));
      }
      return new Mapping(remaining, false);
    } else {
      return generateAlphaPrefix(ctx);
    }
  }

  private boolean matchesInclude(AstNode node, Optional<Set<Class<? extends AstNode>>> include) {
    return (
      include.isEmpty() ||
      include.get().stream().anyMatch(included -> included.isInstance(node)) ||
      (
        node.parent().map(p -> p instanceof AstList).orElse(false) &&
        node.children().size() > 0 &&
        include.get().stream().anyMatch(included -> included.isInstance(node.children().get(0)))
      )
    );
  }

  @SuppressWarnings("unchecked")
  public Optional<Mapping> generateSnippets(
    GenerationContext ctx,
    AstNode node,
    Optional<Set<Class<? extends AstNode>>> include
  ) {
    // Skip nodes that are too large. This constrains to 200-line nodes at 25 tokens a node.
    if (node.tokenRange().map(r -> r.stop - r.start).orElse(0) > 5000) {
      return Optional.empty();
    }

    if (!matchesInclude(node, include)) {
      return Optional.empty();
    }

    // innerElementType nodes of AstLists are only generated from the wrapping position.
    Optional<AstList> grandParent = node
      .parent()
      .flatMap(p -> p.parent())
      .filter(gp -> gp instanceof AstList)
      .map(gp -> (AstList) gp);
    if (grandParent.isPresent()) {
      List<Class<? extends AstParent>> innerElementTypes = grandParent.get().innerElementTypes();
      if (innerElementTypes.stream().anyMatch(c -> c.isInstance(node))) {
        return Optional.empty();
      }
    }

    return snippets.generateSnippet(ctx, node);
  }

  public Optional<Mapping> generateSnippet(GenerationContext ctx) {
    List<AstNode> eligible = ctx
      .eligibleObjects()
      .stream()
      .filter(node -> snippets.map().containsKey(node.getClass()))
      .collect(Collectors.toList());
    if (eligible.size() == 0 || !ctx.sampler.saySnippet()) {
      return Optional.empty();
    }

    AstNode node = random.element(eligible);
    return snippets
      .map()
      .get(node.getClass())
      .apply(ctx, node)
      .map(
        prefix -> {
          Range continuationRange = new Range(
            ctx.tokens.tokenPosition(node.range().stop),
            ctx.eligible.stop
          );
          if (!prefix.partial) {
            Mapping postfix = continueGeneration(ctx, continuationRange);
            // Carry over postfix partial.
            postfix.phrases.addAll(0, prefix.phrases);
            postfix.outputTokens.addAll(0, prefix.outputTokens);
            postfix.remaining.addAll(0, prefix.remaining);
            return postfix;
          }
          if (continuationRange.length() > 0) {
            prefix.remaining.add(continuationRange);
          }
          return prefix;
        }
      );
  }

  private Optional<Integer> sampleAlphaStop(GenerationContext ctx) {
    if (!ctx.allowPartial) {
      return Optional.empty();
    }
    AlphaNumericFinder.AlphaPrefix alphaPrefix = alphaNumericFinder.alphaPrefix(
      ctx.tokens.list.subList(ctx.eligible.start, ctx.eligible.stop)
    );
    for (int i = ctx.eligible.start + 1; i < ctx.eligible.start + alphaPrefix.length; i++) {
      if (ctx.sampler.stop()) {
        return Optional.of(i);
      }
    }
    return Optional.empty();
  }

  private Mapping generateAlphaPrefix(GenerationContext ctx) {
    List<Tokenizer.Token> outputTokens = new ArrayList<>();
    List<String> phrases = new ArrayList<>();

    Optional<Integer> stop = sampleAlphaStop(ctx);
    List<Tokenizer.Token> alphaPrefixTokens = ctx.tokens.list.subList(
      ctx.eligible.start,
      stop.orElse(ctx.eligible.stop)
    );
    AlphaNumericFinder.AlphaPrefix alphaPrefix = alphaNumericFinder.alphaPrefix(alphaPrefixTokens);
    alphaPrefix = sampleAlternativeAlphaPrefix(ctx, alphaPrefix);
    alphaPrefixTokens = alphaPrefixTokens.subList(0, alphaPrefix.length);

    boolean sayDelimiters = false;
    // Explicitly say the style some of the time.
    if (explicitStyle(ctx, alphaPrefix.style)) {
      // Using underscore is an alternative way of saying these styles.
      if (
        alphaPrefix.length != 1 &&
        (alphaPrefix.style == TextStyle.UNDERSCORES || alphaPrefix.style == TextStyle.DASHES) &&
        random.bool(0.5)
      ) {
        sayDelimiters = true;
      } else {
        phrases.add(random.element(styleToNames.get(alphaPrefix.style)));
      }
    }

    // Say the actual phrases, numbers, and occasionally the delimiters.
    for (int j = 0; j < alphaPrefixTokens.size(); j++) {
      Tokenizer.Token token = alphaPrefixTokens.get(j);
      if (token instanceof Tokenizer.AlphaToken) {
        Tokenizer.AlphaToken alphaToken = ((Tokenizer.AlphaToken) token);
        if (
          random.bool(0.02) && compositeWords.isCompositeWord(alphaToken.word(), random.bool(0.15))
        ) {
          List<String> compositeWord = random.element(compositeWords.sequences(alphaToken.word()));
          if (random.bool(0.67)) {
            // Make the "one word" implicit 1/3 of the time, then decide if we're going to
            // use the standard "one word" phrasing, or use "lower case" some of the time when it's
            // applicable.
            if (j == 0 && alphaPrefix.style == TextStyle.LOWERCASE && random.bool(0.5)) {
              phrases.add(random.element(styleToNames.get(TextStyle.LOWERCASE)));
            } else {
              phrases.addAll(Arrays.asList("one", "word"));
            }
          }
          phrases.addAll(compositeWord);
          outputTokens.addAll(compositeWordTokens(alphaToken, compositeWord));
          continue;
        } else {
          String word = sampleAliases(alphaToken.word());
          if (config.sampleAlternativeWords) {
            word = alternativeWordSampler.sample(word);
          }
          phrases.add(word);
        }
      } else if (token instanceof Tokenizer.NumberToken) {
        phrases.addAll(sampleEnglish((Tokenizer.NumberToken) token));
      } else if (sayDelimiters) {
        if (random.bool(0.8)) {
          phrases.add(random.element(symbolToPhrases.get(token.originalCode())));
        }
      }
      outputTokens.add(token);
    }
    if (stop.isPresent()) {
      return new Mapping(
        phrases,
        outputTokens,
        Arrays.asList(new Range(stop.get(), ctx.eligible.stop)),
        true
      );
    }
    return continueGeneration(ctx, phrases, outputTokens, alphaPrefix.length);
  }

  private AlphaNumericFinder.AlphaPrefix sampleAlternativeAlphaPrefix(
    GenerationContext ctx,
    AlphaNumericFinder.AlphaPrefix alphaPrefix
  ) {
    if (alphaPrefix.style == TextStyle.TITLE_CASE && ctx.sampler.titleCaseAsCapitals()) {
      return new AlphaNumericFinder.AlphaPrefix(TextStyle.CAPITALIZED, 1);
    }

    // Very rarely sample "foo" as "camel case foo" and "Foo" as "pascal case foo"
    TextStyle style = alphaPrefix.style;
    if (alphaPrefix.length == 1 && random.bool(0.015)) {
      // Don't sample in this way if there's another lowercase word after this word.
      if (
        style == TextStyle.LOWERCASE &&
        !(
          ctx.eligible.start + 2 < ctx.eligible.stop &&
          (
            ctx.tokens.list.get(ctx.eligible.start + 1) instanceof Tokenizer.SpaceToken &&
            ctx.tokens.list.get(ctx.eligible.start + 2) instanceof Tokenizer.AlphaToken
          )
        )
      ) {
        style = random.element(Arrays.asList(TextStyle.UNDERSCORES, TextStyle.CAMEL_CASE));
      } else if (style == TextStyle.CAPITALIZED) {
        style = random.element(Arrays.asList(TextStyle.PASCAL_CASE, TextStyle.TITLE_CASE));
      }
    }
    return new AlphaNumericFinder.AlphaPrefix(style, alphaPrefix.length);
  }

  private boolean explicitStyle(GenerationContext ctx, TextStyle style) {
    return ctx.sampler.explicitStyle(
      ctx.astObjects
        .map(
          a ->
            a.textPositions.contains(
              ((Tokenizer.CodeToken) ctx.tokens.list.get(ctx.eligible.start)).range.start
            )
        )
        .orElse(false),
      style
    );
  }

  private List<Tokenizer.Token> compositeWordTokens(
    Tokenizer.AlphaToken originalToken,
    List<String> compositeWord
  ) {
    List<Tokenizer.Token> ret = new ArrayList<>();
    Range range = new Range(
      originalToken.range.start,
      originalToken.range.start + compositeWord.get(0).length()
    );
    ret.add(new Tokenizer.AlphaToken(originalToken.source, range, originalToken.style));
    for (int i = 1; i < compositeWord.size(); i++) {
      range = new Range(range.stop, range.stop + compositeWord.get(i).length());
      ret.add(
        new Tokenizer.AlphaToken(
          originalToken.source,
          range,
          originalToken.style == AlphaStyle.CAPITAL ? AlphaStyle.LOWERCASE : originalToken.style
        )
      );
    }
    return ret;
  }

  private Optional<Mapping> generateSymbol(GenerationContext ctx) {
    String symbols = consecutiveSymbols(ctx, ctx.eligible.start).get();
    // Implicit underscore if we're starting in the middle of a variable.
    // Note that there's no need to check that eligible.start is the start of the
    // insert mapping. It's implied from how we handle underscores in the alphaPrefix
    // codepath.
    if (
      ctx.eligible.start > 0 &&
      ctx.eligible.start + 2 < ctx.tokens.list.size() &&
      symbols.equals("_") &&
      random.bool(0.8)
    ) {
      return Optional.of(
        continueGeneration(
          ctx,
          Collections.emptyList(),
          Arrays.asList(ctx.tokens.list.get(ctx.eligible.start)),
          symbols.length()
        )
      );
    }

    // Use a random applicable symbol.
    String symbol = randomConsumableSymbol(symbols, symbolToPhrases.keySet()).orElse(null);
    if (symbol == null) {
      return Optional.empty();
    }
    List<String> phrases;
    if (
      symbolToPrioritizedPhrases.containsKey(symbol) && ThreadLocalRandom.current().nextInt(10) != 0
    ) {
      phrases = symbolToPrioritizedPhrases.get(symbol);
    } else {
      phrases = symbolToPhrases.get(symbol);
    }
    String phrase = random.element(phrases);
    if (random.bool(0.01)) {
      phrase += " sign";
    }

    List<Tokenizer.Token> outputTokens = ctx.tokens.list.subList(
      ctx.eligible.start,
      ctx.eligible.start + symbol.length()
    );

    Boolean explicit = ctx.sampler.explicitSymbol(symbol);
    Mapping ret = continueGeneration(ctx, symbol.length());
    ret.outputTokens.addAll(0, outputTokens);
    if (explicit || (!ctx.allowTrailingImplicit && ret.phrases.size() == 0)) {
      ret.phrases.add(0, phrase);
    }
    return Optional.of(ret);
  }

  private Optional<Mapping> generateEnclosure(GenerationContext ctx) {
    String symbols = consecutiveSymbols(ctx, ctx.eligible.start).get();

    // Use a random applicable enclosure. Occasionally return instead so that we can use
    // the regular symbol codepath instead.
    String enclosureStart = randomConsumableSymbol(symbols, enclosureStartToEnd.keySet())
      .orElse("");
    if (enclosureStart.equals("")) {
      return Optional.empty();
    }
    Range startRange = new Range(ctx.eligible.start, ctx.eligible.start + enclosureStart.length());
    String enclosureEnd = enclosureStartToEnd.get(enclosureStart);
    Range stopRange = enclosureStopRange(ctx, enclosureStart, enclosureEnd).orElse(null);
    if (stopRange == null || random.bool(0.1)) {
      return Optional.empty();
    }

    String name;
    if (
      ((enclosureStart.equals("(") || enclosureStart.equals("<")) && random.bool(0.8)) ||
      (enclosureStart.equals("[") && random.bool(0.1))
    ) {
      name = "of";
    } else {
      name = random.element(enclosureStartToNames.get(enclosureStart));
    }

    Range eligibleBeforeStop = new Range(startRange.stop, stopRange.start);
    Range eligibleAfterStop = new Range(stopRange.stop, ctx.eligible.stop);

    GenerationContext innerCtx = new GenerationContext(ctx);
    innerCtx.allowTrailingImplicit = true;
    Mapping insideEnclosureMapping = continueGeneration(innerCtx, eligibleBeforeStop);
    insideEnclosureMapping.outputTokens.addAll(
      0,
      ctx.tokens.list.subList(startRange.start, startRange.stop)
    );
    insideEnclosureMapping.outputTokens.addAll(
      ctx.tokens.list.subList(stopRange.start, stopRange.stop)
    );

    Mapping afterEnclosureMapping = new Mapping(Arrays.asList(eligibleAfterStop), false); // empty by default
    // Handle these two cases:
    // 1. Stopping short of stop: "foo(a + b) + c + d" --> "foo of a", "plus b", "plus c plus d"
    // 2. Continuing past the stop: "foo(a + b) + c + d" --> "foo of a plus b plus c", "plus d"
    if (!insideEnclosureMapping.partial) {
      afterEnclosureMapping = continueGeneration(ctx, eligibleAfterStop);
    }
    Mapping result = insideEnclosureMapping;
    if (
      ctx.sampler.explicitEnclosures(enclosureStart) ||
      (!ctx.allowTrailingImplicit && insideEnclosureMapping.phrases.size() == 0)
    ) {
      boolean postfix = false;
      if (name.startsWith("in ")) {
        boolean inEnclosureAsPostfix = ctx.sampler.inEnclosureAsPostfix();
        if (inEnclosureAsPostfix) {
          postfix = true;
        } else if (
          ctx.sampler.stripInFromEnclosure(insideEnclosureMapping.outputTokens.size() == 0)
        ) {
          // Alias "foo in parens bar" to "foo parens bar".
          name = name.substring("in ".length(), name.length());
        }
      }

      if (postfix) {
        result.phrases.add(name);
      } else {
        result.phrases.add(0, name);
      }
    }
    result.phrases.addAll(afterEnclosureMapping.phrases);
    result.outputTokens.addAll(afterEnclosureMapping.outputTokens);
    result.remaining.addAll(afterEnclosureMapping.remaining);

    return Optional.of(result);
  }

  private Optional<Range> enclosureStopRange(
    GenerationContext ctx,
    String enclosureStart,
    String enclosureEnd
  ) {
    int i = ctx.eligible.start + enclosureStart.length();
    int openCount = 1;
    while (i <= ctx.eligible.stop - enclosureEnd.length()) {
      Optional<String> symbolsPrefix = consecutiveSymbols(ctx, i);
      // Check end first in case the start and end tokens are the same.
      if (symbolsPrefix.isPresent() && symbolsPrefix.get().startsWith(enclosureEnd)) {
        openCount--;
        if (openCount == 0) {
          return Optional.of(new Range(i, i + enclosureEnd.length()));
        }
        i += enclosureEnd.length();
      } else if (symbolsPrefix.isPresent() && symbolsPrefix.get().startsWith(enclosureStart)) {
        openCount++;
        i += enclosureStart.length();
      } else {
        i++;
      }
    }
    return Optional.empty();
  }

  private Optional<String> consecutiveSymbols(GenerationContext ctx, int index) {
    String symbols = "";
    // Consume consecutive symbol tokens.
    while (
      index + symbols.length() < ctx.eligible.stop &&
      ctx.tokens.list.get(index + symbols.length()) instanceof Tokenizer.SymbolToken
    ) {
      symbols += ctx.tokens.list.get(index + symbols.length()).originalCode();
    }
    return Optional.of(symbols).filter(s -> !s.equals(""));
  }

  private Optional<String> randomConsumableSymbol(
    String line,
    Collection<String> candidateSymbols
  ) {
    List<String> symbols = new ArrayList<>();
    for (String candidateSymbol : candidateSymbols) {
      if (line.startsWith(candidateSymbol)) {
        symbols.add(candidateSymbol);
      }
    }
    if (symbols.size() > 0) {
      String symbol = random.element(symbols);
      return Optional.of(symbol);
    }
    return Optional.empty();
  }
}
