package core.codeengine;

import core.formattedtext.DefaultConversionMap;
import core.gen.rpc.Language;
import core.util.TextStyler;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Escaper {

  @Inject
  DefaultConversionMap conversionMap;

  public static String prefix = "ESCAPE_";

  @Inject
  public Escaper() {}

  public List<String> escapeWords(Language language, List<String> words) {
    List<List<String>> escapePhrases = conversionMap.escapePrefixes;
    List<String> ret = new ArrayList<>();
    int i = 0;
    while (i < words.size()) {
      List<String> escapePhraseFound = null;
      for (List<String> escapePhrase : escapePhrases) {
        if (
          i + escapePhrase.size() < words.size() &&
          words.subList(i, i + escapePhrase.size()).equals(escapePhrase)
        ) {
          escapePhraseFound = escapePhrase;
          break;
        }
      }
      if (escapePhraseFound == null) {
        ret.add(words.get(i));
        i++;
      } else {
        ret.add(prefix + words.get(i + escapePhraseFound.size()));
        i += escapePhraseFound.size() + 1;
      }
    }
    return ret;
  }

  public String unescapeWord(String word) {
    if (word.startsWith(prefix)) {
      word = word.substring(prefix.length(), word.length());
    }
    return word;
  }
}
