package core.codeengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UnknownReplacer {

  public static class HitMaxUnknowns extends RuntimeException {

    public HitMaxUnknowns() {
      super("Cannot have any more unknowns");
    }
  }

  public class StringsWithUnknowns {

    public final List<String> strings;
    public final Map<String, Integer> unknowns;

    StringsWithUnknowns(List<String> strings, Map<String, Integer> unknowns) {
      this.strings = strings;
      this.unknowns = unknowns;
    }
  }

  private final int maxUnknowns = 20;
  private final boolean deterministic = System.getenv("DETERMINISTIC_UNKNOWNS") != null;
  private final Set<String> lexicon;

  public UnknownReplacer(Set<String> lexicon) {
    this.lexicon = lexicon;
  }

  public List<String> additionalUnknowns(StringsWithUnknowns input, String string) {
    // unknowns that we don't expect to be here.
    return Arrays
      .asList(string.split(" "))
      .stream()
      .filter(
        s -> !input.unknowns.keySet().contains(s) && !lexicon.contains(s) && !s.startsWith("UNK")
      )
      .collect(Collectors.toList());
  }

  public StringsWithUnknowns stringsWithUnknowns(List<String> strings) {
    List<String> replacedStrings = new ArrayList<>();
    List<Integer> unknownIds = IntStream.range(0, maxUnknowns).boxed().collect(Collectors.toList());
    if (!deterministic) {
      Collections.shuffle(unknownIds);
    }
    int nextUnknownIdIndex = 0;
    Map<String, Integer> unknowns = new HashMap<>();
    for (String sentence : strings) {
      List<String> ret = new ArrayList<>();
      for (String s : sentence.split(" ")) {
        if (lexicon.contains(s)) {
          ret.add(s);
          continue;
        }
        if (!unknowns.containsKey(s)) {
          if (nextUnknownIdIndex == unknownIds.size()) {
            throw new HitMaxUnknowns();
          }
          Integer unknownId = unknownIds.get(nextUnknownIdIndex++);
          unknowns.put(s, unknownId);
        }
        ret.add("UNK" + unknowns.get(s));
      }
      replacedStrings.add(ret.stream().collect(Collectors.joining(" ")));
    }
    return new StringsWithUnknowns(replacedStrings, unknowns);
  }

  public String resolveUnknowns(String string, Map<String, Integer> unknowns) {
    for (Map.Entry<String, Integer> unknown : unknowns.entrySet()) {
      int number = unknown.getValue(); // unbox to prevent null, etc.
      string =
        string.replaceAll("\\bUNK" + number + "\\b", Matcher.quoteReplacement(unknown.getKey()));
    }

    return string;
  }

  public String replaceUnknowns(String string, Map<String, Integer> unknowns) {
    for (Map.Entry<String, Integer> unknown : unknowns.entrySet()) {
      string =
        string.replaceAll(
          "\\b" + Pattern.quote(unknown.getKey()) + "\\b",
          Matcher.quoteReplacement("UNK" + unknown.getValue())
        );
    }
    return string;
  }
}
