package core.evaluator;

import core.formattedtext.ConversionMap;
import core.formattedtext.ConversionMapFactory;
import core.gen.rpc.Language;
import core.util.FillerWords;
import core.util.NumberConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import speechengine.gen.rpc.Alternative;

@Singleton
public class TranscriptNormalizer {

  private Pattern wordOrContraction = Pattern.compile("^([a-z][a-z']*)|([a-z']*[a-z])$");

  private Map<String, String> symbolToPhrase = new HashMap<>();

  @Inject
  FillerWords fillerWords;

  @Inject
  NumberConverter numberConverter;

  @Inject
  public TranscriptNormalizer(ConversionMapFactory conversionMapFactory) {
    ConversionMap conversionMap = conversionMapFactory.create(Language.LANGUAGE_DEFAULT);
    for (Map.Entry<List<String>, String> entry : conversionMap.symbolMap.entrySet()) {
      String replacement = entry.getKey().stream().collect(Collectors.joining(" "));
      String symbol = entry.getValue();
      if (!this.symbolToPhrase.containsKey(symbol)) {
        this.symbolToPhrase.put(symbol, replacement);
      }
    }

    this.symbolToPhrase.put(".", "dot");
  }

  private List<String> tokenize(String transcript) {
    // Tokenizes words into contractions
    List<String> tokenized = new ArrayList<>();
    for (String word : Arrays.asList(transcript.split("\\s+"))) {
      if (wordOrContraction.matcher(word).matches()) {
        tokenized.add(word);
      } else {
        tokenized.addAll(Arrays.asList(word.split("(?![a-z])|(?<![a-z])")));
      }
    }
    return tokenized;
  }

  private String convertSymbols(String symbol) {
    if (symbolToPhrase.containsKey(symbol)) {
      return symbolToPhrase.get(symbol).trim();
    }
    return symbol;
  }

  private String normalize(String transcript) {
    // Input transcripts must be normalized before sending to the transcript parser model.
    // Allowed characters are: <space>, <apostrophe> when in a contraction, a-z.
    // Digits and symbols should be converted to their text representation.
    transcript = fillerWords.strip(transcript.toLowerCase().trim());

    // We need to replace digits in the transcripts since the model is not trained on them.
    transcript =
      tokenize(transcript)
        .stream()
        .map(token -> token.matches("[0-9]+") ? numberConverter.fromDigitsToText(token) : token)
        .map(token -> token.matches("[^a-z]") ? convertSymbols(token) : token)
        .collect(Collectors.joining(" "));
    return transcript;
  }

  public List<Alternative> normalize(List<Alternative> alternatives) {
    // filter out transcripts that are empty as a result of normalization. e.g. "uh".
    return alternatives
      .stream()
      .map(a -> Alternative.newBuilder(a).setTranscript(normalize(a.getTranscript())).build())
      .filter(a -> !a.getTranscript().equals(""))
      .collect(Collectors.toList());
  }
}
