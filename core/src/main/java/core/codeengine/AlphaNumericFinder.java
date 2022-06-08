package core.codeengine;

import core.gen.rpc.Language;
import core.util.Range;
import core.util.TextStyle;
import core.util.TextStyler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AlphaNumericFinder {

  public static class AlphaPrefix {

    public final TextStyle style;
    public final Integer length;

    public AlphaPrefix(TextStyle style, Integer length) {
      this.style = style;
      this.length = length;
    }
  }

  @Inject
  public AlphaNumericFinder() {}

  public AlphaPrefix alphaPrefix(List<Tokenizer.Token> tokens) {
    Map<TextStyle, Integer> prefixLength = new HashMap<TextStyle, Integer>();
    prefixLength.put(
      TextStyle.ALL_CAPS,
      alphaStyleDelimitedPrefixLength(tokens, AlphaStyle.CAPS, "_", false)
    );
    prefixLength.put(
      TextStyle.UNDERSCORES,
      alphaStyleDelimitedPrefixLength(tokens, AlphaStyle.LOWERCASE, "_", true)
    );
    prefixLength.put(
      TextStyle.DASHES,
      alphaStyleDelimitedPrefixLength(tokens, AlphaStyle.LOWERCASE, "-", true)
    );
    prefixLength.put(
      TextStyle.TITLE_CASE,
      alphaStyleDelimitedPrefixLength(tokens, AlphaStyle.CAPITAL, " ", true)
    );
    AlphaStyle firstStyle = ((Tokenizer.AlphaToken) tokens.get(0)).style;
    if (
      firstStyle == AlphaStyle.CAPITAL ||
      (tokens.get(0).originalCode().length() == 1 && firstStyle == AlphaStyle.CAPS)
    ) {
      prefixLength.put(TextStyle.PASCAL_CASE, capitalsPrefixLength(tokens));
      prefixLength.put(TextStyle.CAPITALIZED, 1);
    } else if (firstStyle == AlphaStyle.LOWERCASE) {
      prefixLength.put(TextStyle.LOWERCASE, 1);
      prefixLength.put(TextStyle.CAMEL_CASE, capitalsPrefixLength(tokens));
    }

    // It's unclear if this sampling does anything now, due to the strictness
    // of our matching.
    int maxLength = prefixLength
      .entrySet()
      .stream()
      .map(e -> e.getValue())
      .max(Integer::compare)
      .get();
    List<TextStyle> styles = prefixLength
      .entrySet()
      .stream()
      .filter(e -> e.getValue() == maxLength)
      .map(e -> e.getKey())
      .collect(Collectors.toList());
    TextStyle style = styles.get(ThreadLocalRandom.current().nextInt(styles.size()));
    return new AlphaPrefix(style, prefixLength.get(style));
  }

  private int alphaStyleDelimitedPrefixLength(
    List<Tokenizer.Token> tokens,
    AlphaStyle alphaStyle,
    String delimiter,
    boolean requireMultipleWords
  ) {
    if (((Tokenizer.AlphaToken) tokens.get(0)).style != alphaStyle) {
      return 0;
    }
    int i = 1;
    while (i < tokens.size()) {
      if (tokens.get(i) instanceof Tokenizer.NumberToken) {
        i++;
      } else if (
        i < tokens.size() - 1 &&
        tokens.get(i).originalCode().equals(delimiter) &&
        tokens.get(i + 1) instanceof Tokenizer.AlphaToken &&
        ((Tokenizer.AlphaToken) tokens.get(i + 1)).style == alphaStyle
      ) {
        i += 2;
      } else {
        break;
      }
    }
    if (requireMultipleWords && i == 1) {
      i = 0;
    }
    return i;
  }

  private int capitalsPrefixLength(List<Tokenizer.Token> tokens) {
    int i = 1;
    boolean foundAlpha = false;
    while (i < tokens.size()) {
      if (
        tokens.get(i) instanceof Tokenizer.AlphaToken &&
        (
          ((Tokenizer.AlphaToken) tokens.get(i)).style == AlphaStyle.CAPITAL ||
          ((Tokenizer.AlphaToken) tokens.get(i)).style == AlphaStyle.CAPS
        )
      ) {
        foundAlpha = true;
      } else if (!(tokens.get(i) instanceof Tokenizer.NumberToken)) {
        break;
      }
      i++;
    }
    if (!foundAlpha) {
      i = 0;
    }
    return i;
  }

  public Map<List<Tokenizer.Token>, List<String>> alphaNumerics(
    List<Tokenizer.Token> priorContext
  ) {
    Map<List<Tokenizer.Token>, List<String>> ret = new HashMap<>();

    while (priorContext.size() > 0) {
      if (!(priorContext.get(0) instanceof Tokenizer.AlphaToken)) {
        priorContext = priorContext.subList(1, priorContext.size());
        continue;
      }
      AlphaPrefix alphaPrefix = alphaPrefix(priorContext);
      List<Tokenizer.Token> tokens = priorContext.subList(0, alphaPrefix.length);
      if (!ret.containsKey(tokens)) {
        List<String> subsequenceWithoutStyling = tokens
          .stream()
          .filter(t -> t instanceof Tokenizer.AlphaToken || t instanceof Tokenizer.NumberToken)
          .map(
            t -> {
              if (t instanceof Tokenizer.AlphaToken) {
                return t.originalCode().toLowerCase();
              } else {
                // Number token. Put digits with spaces in.
                // FIXME: this was the behavior before but it doesn't look right.
                return t.modelCodeRepresentation();
              }
            }
          )
          .collect(Collectors.toList());
        ret.put(tokens, subsequenceWithoutStyling);
      }
      priorContext = priorContext.subList(alphaPrefix.length, priorContext.size());
    }
    return ret;
  }
}
