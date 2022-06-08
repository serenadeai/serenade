package core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PhraseHintExtractor {

  @Inject
  public PhraseHintExtractor() {}

  private final int phraseHintsLimit = 500;
  public static int customHintsLimit = 50;

  private Map<String, Long> alphanumericCounts(String source) {
    Pattern p = Pattern.compile("[a-zA-Z_]+");
    Matcher m = p.matcher(source);
    List<String> phrases = new ArrayList<>();
    while (m.find()) {
      phrases.addAll(Arrays.asList(removeSourceStyling(m.group()).split("\\s+")));
    }

    return phrases
      .stream()
      .filter(s -> s.length() > 0)
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
      .entrySet()
      .stream()
      .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
      .limit(phraseHintsLimit)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private String removeHintStyling(String s) {
    s = s.replaceAll("\\d", "");
    s = s.replaceAll("\\s", "");
    return s.toLowerCase();
  }

  private String removeSourceStyling(String s) {
    s = s.replaceAll("([a-z])([A-Z])", "$1 $2");
    s = s.replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
    s = s.replaceAll("_", " ");
    s = s.toLowerCase();
    return s;
  }

  public List<String> extract(String source, List<String> hints) {
    return new ArrayList<>(
      Stream
        .concat(
          alphanumericCounts(source).entrySet().stream().map(Map.Entry::<String, Long>getKey),
          hints.stream().map(s -> removeHintStyling(s)).limit(customHintsLimit)
        )
        .collect(Collectors.toSet())
    );
  }
}
