package core.codeengine;

import codeengine.gen.rpc.Model;
import codeengine.gen.rpc.RescoringAlternative;
import codeengine.gen.rpc.RescoringRequest;
import codeengine.gen.rpc.RescoringResponse;
import codeengine.gen.rpc.TranslationAlternative;
import codeengine.gen.rpc.TranslationOutput;
import codeengine.gen.rpc.TranslationRequest;
import codeengine.gen.rpc.TranslationResponse;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import core.gen.rpc.Language;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolbelt.client.ServiceHttpClient;
import toolbelt.languages.LanguageDeterminer;
import toolbelt.logging.Logs;

@Singleton
public class CodeEngineClient {

  private static class SlotParse {

    public final List<Tokenizer.Token> priorContext;
    public final Map<List<Tokenizer.Token>, List<String>> alphaNumerics;

    public SlotParse(
      List<Tokenizer.Token> priorContext,
      Map<List<Tokenizer.Token>, List<String>> alphaNumerics
    ) {
      this.priorContext = priorContext;
      this.alphaNumerics = alphaNumerics;
    }
  }

  // Truncate at ~1%. Could technically happen in reranker, but be a decent sized change.
  private final double alternativeThreshold = -4.5;
  private Logger logger = LoggerFactory.getLogger(CodeEngineClient.class);
  public static int maxPriorContextSize = 35;

  private ServiceHttpClient serviceHttpClient;
  private InputConverter inputConverter;
  private Escaper escaper;
  private LanguageDeterminer languageDeterminer;
  private Tokenizer tokenizer;
  private Map<Model, Map<Language, UnknownReplacer>> unknownReplacers = new HashMap<>();
  private UnknownReplacer emptyReplacer = new UnknownReplacer(new HashSet<>());

  @Inject
  public CodeEngineClient(
    ServiceHttpClient serviceHttpClient,
    InputConverter inputConverter,
    Escaper escaper,
    LanguageDeterminer languageDeterminer,
    Tokenizer tokenizer
  ) {
    this.serviceHttpClient = serviceHttpClient;
    this.inputConverter = inputConverter;
    this.escaper = escaper;
    this.languageDeterminer = languageDeterminer;
    this.tokenizer = tokenizer;

    unknownReplacers.put(Model.MODEL_TRANSCRIPT_PARSER, new HashMap<>());
    try {
      unknownReplacers
        .get(Model.MODEL_TRANSCRIPT_PARSER)
        .put(
          Language.LANGUAGE_DEFAULT,
          new UnknownReplacer(
            new HashSet<>(
              Arrays.asList(
                Resources
                  .toString(
                    Resources.getResource("lexicons/default_transcript_parser_lexicon.txt"),
                    Charsets.UTF_8
                  )
                  .split("\n")
              )
            )
          )
        );
    } catch (IOException e) {
      throw new RuntimeException("Error loading transcript parser lexicon", e);
    }

    unknownReplacers.put(Model.MODEL_AUTO_STYLE, new HashMap<>());
    for (Language language : languageDeterminer.languages()) {
      try {
        String languageName = language.toString().split("_")[1].toLowerCase();
        unknownReplacers
          .get(Model.MODEL_AUTO_STYLE)
          .put(
            language,
            new UnknownReplacer(
              new HashSet<>(
                Arrays.asList(
                  Resources
                    .toString(
                      Resources.getResource("lexicons/" + languageName + "_auto_style_lexicon.txt"),
                      Charsets.UTF_8
                    )
                    .split("\n")
                )
              )
            )
          );
      } catch (IOException e) {
        throw new RuntimeException("Error loading lexicon: " + language, e);
      }
    }
  }

  private Map<String, SlotParse> slotParses(
    List<SlotContext> slotContexts,
    boolean includeAlphaNumeric
  ) {
    Map<String, SlotParse> result = new HashMap<>();
    for (SlotContext slotContext : slotContexts) {
      String prefix = slotPrefix(slotContext);
      if (!result.containsKey(prefix)) {
        List<Tokenizer.Token> priorContext = tokenizer.tokenizeSourcePrefix(
          slotContext.source.substring(0, slotContext.slotStart)
        );

        result.put(
          prefix,
          new SlotParse(
            priorContext,
            includeAlphaNumeric
              ? inputConverter.alphaNumerics(priorContext, maxPriorContextSize)
              : Collections.emptyMap()
          )
        );
      }
    }

    return result;
  }

  private String slotPrefix(SlotContext slotContext) {
    return slotContext.source.substring(0, slotContext.slotStart);
  }

  private UnknownReplacer unknownReplacer(Model model, Language language) {
    return unknownReplacers.getOrDefault(model, Map.of(language, emptyReplacer)).get(language);
  }

  public CompletableFuture<List<List<TranslationAlternative>>> translate(
    Language language,
    List<SlotContext> slotContexts
  ) {
    return Logs.logTime(
      logger,
      "core.code-engine-translate",
      Map.of(),
      () -> {
        if (slotContexts.size() == 0) {
          return CompletableFuture.completedFuture(Collections.emptyList());
        }

        Map<String, SlotParse> slotParses = slotParses(slotContexts, true);
        List<UnknownReplacer.StringsWithUnknowns> inputs = new ArrayList<>();
        for (SlotContext slotContext : slotContexts) {
          SlotParse slotParse = slotParses.get(slotPrefix(slotContext));
          List<String> words = Arrays.asList(slotContext.english.split(" "));
          inputs.add(
            unknownReplacer(Model.MODEL_AUTO_STYLE, language)
              .stringsWithUnknowns(
                Arrays.asList(
                  inputConverter
                    .convert(
                      slotParse.priorContext,
                      slotParse.alphaNumerics,
                      escaper.escapeWords(language, words),
                      maxPriorContextSize,
                      1.0,
                      slotContext.snippetContainer
                    )
                    .modelCodeRepresentation()
                )
              )
          );
        }

        CompletableFuture<TranslationResponse> response = serviceHttpClient.post(
          ServiceHttpClient.Service.CodeEngine,
          "/api/translate",
          TranslationRequest
            .newBuilder()
            .setModel(Model.MODEL_AUTO_STYLE)
            .setLanguage(language)
            .addAllInputSentence(
              inputs.stream().map(s -> s.strings.get(0)).collect(Collectors.toList())
            )
            .build(),
          TranslationResponse.parser()
        );

        CompletableFuture<List<List<TranslationAlternative>>> result = response.thenApply(
          innerResponse -> {
            List<List<TranslationAlternative>> innerResult = new ArrayList<>();
            for (int i = 0; i < inputs.size(); i++) {
              TranslationOutput output = innerResponse.getOutput(i);
              List<TranslationAlternative> currentResults = new ArrayList<>();
              for (TranslationAlternative alternative : output.getAlternativeList()) {
                if (
                  alternative.getScore() > alternativeThreshold &&
                  !alternative.getSentence().contains("NL NL NL NL") &&
                  !alternative.getSentence().contains("SP SP SP SP SP SP SP SP")
                ) {
                  currentResults.add(
                    TranslationAlternative
                      .newBuilder()
                      .setSentence(
                        tokenizer.decodeModelCodeRepresentation(
                          unknownReplacer(Model.MODEL_AUTO_STYLE, language)
                            .resolveUnknowns(alternative.getSentence(), inputs.get(i).unknowns)
                        )
                      )
                      .setScore(alternative.getScore())
                      .build()
                  );
                }
              }

              innerResult.add(currentResults);
            }

            return innerResult;
          }
        );

        return result;
      }
    );
  }

  public CompletableFuture<List<RescoringAlternative>> rescore(
    Language language,
    List<SlotContext> slotContexts
  ) {
    return Logs.logTime(
      logger,
      "core.code-engine-rescore",
      Map.of(),
      () -> {
        if (slotContexts.size() == 0) {
          return CompletableFuture.completedFuture(Collections.emptyList());
        }

        RescoringRequest.Builder rescoringRequestBuilder = RescoringRequest
          .newBuilder()
          .setModel(Model.MODEL_CONTEXTUAL_LANGUAGE_MODEL)
          .setLanguage(language);

        Map<String, SlotParse> slotParses = slotParses(slotContexts, false);
        for (SlotContext slotContext : slotContexts) {
          SlotParse slotParse = slotParses.get(slotPrefix(slotContext));
          String inputSentence = inputConverter
            .convert(
              slotParse.priorContext,
              Collections.emptyMap(),
              Collections.emptyList()/* input transcript, unused by leading context */,
              maxPriorContextSize,
              1.0,
              slotContext.snippetContainer
            )
            .leadingContextRepresentation();
          String outputSentence = slotContext.english;

          rescoringRequestBuilder.addAlternative(
            RescoringAlternative
              .newBuilder()
              .setInputSentence(inputSentence)
              .setOutputSentence(outputSentence)
              .build()
          );
        }

        CompletableFuture<RescoringResponse> rescoringResponse = serviceHttpClient.post(
          ServiceHttpClient.Service.CodeEngine,
          "/api/rescore",
          rescoringRequestBuilder.build(),
          RescoringResponse.parser()
        );

        CompletableFuture<List<RescoringAlternative>> result = rescoringResponse.thenApply(
          innerResponse -> {
            List<RescoringAlternative> innerResult = new ArrayList<>();
            for (int r = 0; r < innerResponse.getAlternativeCount(); r++) {
              innerResult.add(innerResponse.getAlternative(r));
            }

            return innerResult;
          }
        );

        return result;
      }
    );
  }

  public List<List<TranslationAlternative>> transcriptParserTranslate(List<String> transcripts) {
    return Logs.logTime(
      logger,
      "core.code-engine-translate-transcript-parser",
      Map.of(),
      () -> {
        UnknownReplacer unknownReplacer = unknownReplacer(
          Model.MODEL_TRANSCRIPT_PARSER,
          Language.LANGUAGE_DEFAULT
        );

        List<UnknownReplacer.StringsWithUnknowns> inputs = new ArrayList<>();
        for (String transcript : transcripts) {
          inputs.add(unknownReplacer.stringsWithUnknowns(Arrays.asList(transcript)));
        }

        List<String> inputSentences = inputs
          .stream()
          .map(s -> s.strings.get(0))
          .collect(Collectors.toList());

        TranslationResponse response = serviceHttpClient.postBlocking(
          ServiceHttpClient.Service.CodeEngine,
          "/api/translate",
          TranslationRequest
            .newBuilder()
            .setModel(Model.MODEL_TRANSCRIPT_PARSER)
            .setLanguage(Language.LANGUAGE_DEFAULT)
            .addAllInputSentence(inputSentences)
            .build(),
          TranslationResponse.parser()
        );

        List<List<TranslationAlternative>> result = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
          TranslationOutput output = response.getOutput(i);
          List<TranslationAlternative> currentResults = new ArrayList<>();
          for (TranslationAlternative alternative : output.getAlternativeList()) {
            currentResults.add(
              TranslationAlternative
                .newBuilder()
                .setSentence(
                  unknownReplacer(Model.MODEL_TRANSCRIPT_PARSER, Language.LANGUAGE_DEFAULT)
                    .resolveUnknowns(alternative.getSentence(), inputs.get(i).unknowns)
                    .strip()
                )
                .setScore(alternative.getScore())
                .build()
            );
          }

          result.add(currentResults);
        }

        return result;
      }
    );
  }
}
