package core.codeengine;

import codeengine.gen.rpc.RescoringAlternative;
import codeengine.gen.rpc.TranslationAlternative;
import core.ast.api.AstParent;
import core.codeengine.CodeEngineBatchQueue;
import core.evaluator.ParsedTranscript;
import core.formattedtext.ConversionMap;
import core.formattedtext.ConversionMapFactory;
import core.formattedtext.FormattedTextConverter;
import core.formattedtext.FormattedTextOptions;
import core.gen.rpc.Language;
import core.metadata.DiffWithMetadata;
import core.util.Diff;
import core.util.Range;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import toolbelt.languages.LanguageDeterminer;

@Singleton
public class Resolver {

  private static class Replacement {

    public final String code;
    // cursor position within the code string
    public final int cursor;

    public Replacement(String code, int cursor) {
      this.code = code;
      this.cursor = cursor;
    }
  }

  @Inject
  ConversionMapFactory conversionMapFactory;

  @Inject
  FormattedTextConverter formattedTextConverter;

  @Inject
  LanguageDeterminer languageDeterminer;

  @Inject
  Whitespace whitespace;

  private final Pattern snippetBasedFormattedTextFeatures = Pattern.compile("\\b(lambda|tag)\\b");

  public static String cursorOverride = "cursorb1f6a678ea69447ba5c56ddafba91d6a";
  public static String indentOverride = "indent4ae5c7a470b94f61a465a87e6d114b5e";
  public static String terminatorOverride = "terminator1edac0dd252141c6b1de624e727a41ea";

  @Inject
  public Resolver() {}

  private String replaceSlots(String generatedWithSlots, Map<String, List<String>> slotsToCode) {
    for (String name : slotsToCode.keySet()) {
      StringBuffer buffer = new StringBuffer();
      Matcher m = Pattern.compile(wrapInSlot("\\s*" + name + "\\s*")).matcher(generatedWithSlots);
      int i = 0;
      while (m.find()) {
        m.appendReplacement(
          buffer,
          Matcher.quoteReplacement(
            slotsToCode.get(name).get(Math.min(i, slotsToCode.get(name).size() - 1))
          )
        );
        i++;
      }

      m.appendTail(buffer);
      generatedWithSlots = buffer.toString();
    }

    return generatedWithSlots;
  }

  private Replacement resolveCursor(String source) {
    int cursor = -1;
    int maxPriority = -1;
    int cursorOffset = 0;
    int cursorLengthSum = 0;
    StringBuffer buffer = new StringBuffer();
    Matcher m = Pattern.compile(wrapInSlot(cursorOverride + "(\\d*)")).matcher(source);
    while (m.find()) {
      int priority = m.group(1).equals("") ? 0 : Integer.parseInt(m.group(1));
      if (priority > maxPriority) {
        cursorOffset = cursorLengthSum;
        maxPriority = priority;
        cursor = m.start();
      }

      cursorLengthSum += m.group().length();
      m.appendReplacement(buffer, "");
    }

    if (cursor > -1) {
      cursor = cursor - cursorOffset;
    }

    m.appendTail(buffer);
    if (cursor == -1) {
      cursor = buffer.toString().length();
    }

    return new Replacement(buffer.toString(), cursor);
  }

  private String resolveIndentsAndTerminators(
    String source,
    String generatedWithSlots,
    ConversionMap conversionMap
  ) {
    String indent = whitespace.indentationToken(source, conversionMap.indentation());
    return generatedWithSlots
      .replace(wrapInSlot(indentOverride), indent)
      .replace(wrapInSlot(terminatorOverride), conversionMap.statementTerminator());
  }

  private String resolveNewlines(String source, Range replacementRange, String generatedWithSlots) {
    return generatedWithSlots.replace(
      "\n",
      "\n" + whitespace.indentationAtCursor(source, replacementRange.start)
    );
  }

  private DiffWithMetadata resolveSlotsWithFormattedText(
    Diff initialState,
    Range replacementRange,
    ConversionMap conversionMap,
    String generatedWithSlots,
    Map<String, String> slotsToEnglish,
    Map<String, List<FormattedTextOptions>> options
  ) {
    Map<String, List<String>> slotsToCode = new HashMap<>();
    for (String name : slotsToEnglish.keySet()) {
      Matcher m = Pattern.compile(wrapInSlot("\\s*" + name + "\\s*")).matcher(generatedWithSlots);
      int i = 0;
      while (m.find()) {
        FormattedTextOptions optionsForSlot = FormattedTextOptions.newBuilder().build();
        if (options.get(name) != null) {
          optionsForSlot = options.get(name).get(Math.min(i, options.get(name).size() - 1));
        }

        if (slotsToCode.get(name) == null) {
          slotsToCode.put(name, new ArrayList<>());
        }

        slotsToCode
          .get(name)
          .add(
            formattedTextConverter.convert(slotsToEnglish.get(name), optionsForSlot, conversionMap)
          );
        i++;
      }
    }

    return resolveReservedSlots(
      initialState,
      replacementRange,
      replaceSlots(generatedWithSlots, slotsToCode),
      conversionMap
    );
  }

  private CompletableFuture<List<DiffWithMetadata>> resolveSlotsWithCodeEngine(
    Diff initialState,
    Range replacementRange,
    Language language,
    ConversionMap conversionMap,
    String generatedWithSlots,
    Map.Entry<String, String> slotToEnglish,
    Optional<AstParent> snippetContainer,
    CodeEngineBatchQueue queue
  ) {
    String generatedWithReservedResolved = resolveIndentsAndTerminators(
      initialState.getSource(),
      generatedWithSlots.replaceAll(wrapInSlot(cursorOverride), ""),
      conversionMap
    );

    SlotContext slotContext = new SlotContext(
      initialState.getSource().substring(0, replacementRange.start) +
      generatedWithReservedResolved.replaceAll(wrapInSlot(".*?"), "") +
      initialState.getSource().substring(replacementRange.stop),
      slotToEnglish.getValue(),
      replacementRange.start +
      generatedWithReservedResolved.indexOf(wrapInSlot(slotToEnglish.getKey())),
      snippetContainer
    );

    CompletableFuture<List<TranslationAlternative>> translationsFuture = queue.translate(
      slotContext
    );

    return queue
      .rescore(slotContext)
      .thenCompose(
        rescoringNullable ->
          translationsFuture.thenApply(
            translations -> {
              Optional<RescoringAlternative> rescoring = Optional.ofNullable(rescoringNullable);
              Optional<Double> contextualLanguageModelCost = rescoring.map(
                e -> (double) -e.getScore()
              );

              return translations
                .stream()
                .map(
                  e -> {
                    DiffWithMetadata result = resolveReservedSlots(
                      initialState,
                      replacementRange,
                      replaceSlots(
                        generatedWithSlots,
                        Map.of(slotToEnglish.getKey(), Arrays.asList(e.getSentence()))
                      ),
                      conversionMap
                    );

                    result.autoStyleCost = Optional.of((double) -e.getScore());
                    result.contextualLanguageModelCost = contextualLanguageModelCost;
                    result.slotContext = Optional.of(slotContext);
                    return result;
                  }
                )
                .collect(Collectors.toList());
            }
          )
      );
  }

  private boolean shouldUseCodeEngine(
    Language language,
    Map<String, String> slotsToEnglish,
    Map<String, List<FormattedTextOptions>> options,
    boolean overrideSingleSlotOptions
  ) {
    if (slotsToEnglish.size() != 1) {
      return false;
    }

    String english = slotsToEnglish.values().iterator().next();
    return (
      !(
        languageDeterminer.languagesWithMlSnippetsDisabled().contains(language) &&
        snippetBasedFormattedTextFeatures.matcher(english).find()
      ) &&
      (overrideSingleSlotOptions || options.isEmpty())
    );
  }

  public CompletableFuture<List<DiffWithMetadata>> resolve(
    Diff initialState,
    Range replacementRange,
    Language language,
    String generatedWithSlots,
    Map<String, String> slotsToEnglish,
    Map<String, List<FormattedTextOptions>> options,
    Optional<AstParent> snippetContainer,
    CodeEngineBatchQueue queue,
    boolean overrideSingleSlotOptions
  ) {
    ConversionMap conversionMap = conversionMapFactory.create(language);

    generatedWithSlots =
      generatedWithSlots
        .replaceAll(wrapInSlot("\\s*cursor(\\d*)\\s*"), wrapInSlot(cursorOverride + "$1"))
        .replaceAll(wrapInSlot("\\s*indent\\s*"), wrapInSlot(indentOverride))
        .replaceAll(wrapInSlot("\\s*terminator\\s*"), wrapInSlot(terminatorOverride))
        .replaceAll(wrapInSlot("\\s*newline(\\d*)\\s*"), "\n"); // snippet api backwards compatibility.

    if (shouldUseCodeEngine(language, slotsToEnglish, options, overrideSingleSlotOptions)) {
      return resolveSlotsWithCodeEngine(
        initialState,
        replacementRange,
        language,
        conversionMap,
        generatedWithSlots,
        slotsToEnglish.entrySet().iterator().next(),
        snippetContainer,
        queue
      );
    }

    // if we have a multi-slot snippet then apply expression styling as the default.
    Map<String, List<FormattedTextOptions>> optionsWithDefaults = new HashMap<>(options);
    for (String slot : slotsToEnglish.keySet()) {
      if (optionsWithDefaults.get(slot) == null) {
        optionsWithDefaults.put(
          slot,
          Arrays.asList(FormattedTextOptions.newBuilder().setExpression(true).build())
        );
      }
    }

    return CompletableFuture.completedFuture(
      Arrays.asList(
        resolveSlotsWithFormattedText(
          initialState,
          replacementRange,
          conversionMap,
          generatedWithSlots,
          slotsToEnglish,
          optionsWithDefaults
        )
      )
    );
  }

  public DiffWithMetadata resolveEnglishWithFormattedText(
    Diff initialState,
    Range replacementRange,
    Language language,
    FormattedTextOptions options,
    String english
  ) {
    ConversionMap conversionMap = conversionMapFactory.create(language);
    return resolveSlotsWithFormattedText(
      initialState,
      replacementRange,
      conversionMap,
      wrapInSlot("snippet"),
      Map.of("snippet", english),
      Map.of("snippet", Arrays.asList(options))
    );
  }

  public CompletableFuture<List<DiffWithMetadata>> resolveEnglish(
    Diff initialState,
    Range replacementRange,
    Language language,
    String english,
    CodeEngineBatchQueue queue
  ) {
    return resolve(
      initialState,
      replacementRange,
      language,
      wrapInSlot("snippet"),
      Map.of("snippet", english),
      Collections.emptyMap(),
      Optional.empty(),
      queue,
      false
    );
  }

  public DiffWithMetadata resolveReservedSlots(
    Diff initialState,
    Range replacementRange,
    String generatedWithReservedSlots,
    ConversionMap conversionMap
  ) {
    Replacement replacement = resolveCursor(
      resolveNewlines(
        initialState.getSource(),
        replacementRange,
        resolveIndentsAndTerminators(
          initialState.getSource(),
          generatedWithReservedSlots,
          conversionMap
        )
      )
    );

    String resolvedDescription = resolveCursor(
      resolveIndentsAndTerminators(
        initialState.getSource(),
        generatedWithReservedSlots,
        conversionMap
      )
    )
      .code;

    return new DiffWithMetadata(
      initialState
        .replaceRange(replacementRange, replacement.code)
        .moveCursor(replacementRange.start + replacement.cursor),
      resolvedDescription
    );
  }

  public static String wrapInSlot(String text) {
    return "<%" + text + "%>";
  }
}
