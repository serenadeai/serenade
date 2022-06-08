package core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NumberConverter {

  private final List<List<String>> escapePhrases = Arrays.asList(
    Arrays.asList("the", "word"),
    Arrays.asList("escape")
  );

  public static final Map<String, Integer> tensPlace = Map.of("hundred", 2, "thousand", 3);

  public static final Map<String, Integer> digits = Map.of(
    "zero",
    0,
    "one",
    1,
    "two",
    2,
    "three",
    3,
    "four",
    4,
    "five",
    5,
    "six",
    6,
    "seven",
    7,
    "eight",
    8,
    "nine",
    9
  );

  public static final Map<String, String> digitToWord = digits
    .entrySet()
    .stream()
    .collect(Collectors.toMap(e -> Integer.toString(e.getValue()), e -> e.getKey()));

  public static final Map<String, Integer> teens = Map.of(
    "ten",
    10,
    "eleven",
    11,
    "twelve",
    12,
    "thirteen",
    13,
    "fourteen",
    14,
    "fifteen",
    15,
    "sixteen",
    16,
    "seventeen",
    17,
    "eighteen",
    18,
    "nineteen",
    19
  );

  public static final Map<String, Integer> tens = Map.of(
    "twenty",
    20,
    "thirty",
    30,
    "forty",
    40,
    "fifty",
    50,
    "sixty",
    60,
    "seventy",
    70,
    "eighty",
    80,
    "ninety",
    90
  );

  public static class Conversion {

    public final int num;
    public final int numTokens;
    public final int numDigits;

    public Conversion(int num, int numTokens, int numDigits) {
      this.num = num;
      this.numTokens = numTokens;
      this.numDigits = numDigits;
    }

    public String formatted() {
      return String.format("%0" + numDigits + "d", num);
    }
  }

  @Inject
  public NumberConverter() {}

  private boolean popEscaped(List<String> s) {
    for (List<String> escapePhrase : escapePhrases) {
      if (
        s.size() - escapePhrase.size() >= 0 &&
        s.subList(s.size() - escapePhrase.size(), s.size()).equals(escapePhrase)
      ) {
        s.subList(s.size() - escapePhrase.size(), s.size()).clear();
        return true;
      }
    }
    return false;
  }

  public String convertNumbers(String s) {
    List<String> numbersReplaced = new ArrayList<>();
    List<String> tokens = Arrays.asList(s.split(" "));
    List<Integer> numberIndices = new ArrayList<>();
    int i = 0;

    // convert numbers with escaping and numbers that stick to each other.
    while (i < tokens.size()) {
      Conversion c;
      if (tokens.get(i).equals("o") && (i == 0 || !numberIndices.contains(i - 1))) {
        // Prevent insert o, system o, and type o from returning 0 when not preceded by another number.
        c = new Conversion(0, 0, 0);
      } else {
        c = fromPrefix(tokens.subList(i, tokens.size()));
      }
      if (c.numTokens > 0) {
        if (popEscaped(numbersReplaced)) {
          numbersReplaced.addAll(tokens.subList(i, i + c.numTokens));
        } else {
          numberIndices.add(numbersReplaced.size());
          numbersReplaced.add(c.formatted());
        }
        i += c.numTokens;
      } else {
        numbersReplaced.add(tokens.get(i));
        i++;
      }
    }

    // stick numbers together.
    String ret = numbersReplaced.get(0);
    for (int j = 1; j < numbersReplaced.size(); j++) {
      if (!(numberIndices.contains(j - 1) && numberIndices.contains(j))) {
        ret += " ";
      }
      ret += numbersReplaced.get(j);
    }
    return ret;
  }

  public boolean isValid(String s) {
    return conversionFromString(s).numTokens == s.split(" ").length;
  }

  public int fromString(String s) {
    return conversionFromString(s).num;
  }

  private Conversion conversionFromString(String s) {
    // already actual number characters.
    if (s.chars().allMatch(Character::isDigit)) {
      return new Conversion(Integer.parseInt(s), 1, s.length());
    }
    List<String> tokens = Arrays.asList(s.split(" "));
    return fromPrefix(tokens);
  }

  private Conversion fromPrefix(List<String> s) {
    if (s.size() == 0) {
      return new Conversion(0, 0, 0);
    }
    int prefix;
    int consumed = 1;
    int prefixNumDigits = 1;
    String token = s.get(0);

    // return if we start with "a", but not followed by "hundred", "thousand", etc.
    if (token.equals("a") && (s.size() == 1 || !tensPlace.containsKey(s.get(1)))) {
      return new Conversion(0, 0, 0);
    }
    if (token.equals("a")) {
      prefix = 1;
    } else if (token.equals("o")) {
      prefix = 0;
    } else if (digits.containsKey(token)) {
      prefix = digits.get(token);
    } else if (tens.containsKey(token)) {
      prefix = tens.get(token);
      // numbers like 23.
      if (s.size() > consumed && digits.containsKey(s.get(consumed))) {
        prefix += digits.get(s.get(1));
        consumed++;
      }
      prefixNumDigits = 2;
    } else if (teens.containsKey(token)) {
      prefix = teens.get(token);
      prefixNumDigits = 2;
    } else {
      return new Conversion(0, 0, 0);
    }

    int minPostfixNumDigits = 0;
    if (consumed < s.size()) {
      boolean tensPlaceFound = false;

      // support things like "hundred thousand"
      while (consumed < s.size() && tensPlace.containsKey(s.get(consumed))) {
        minPostfixNumDigits += tensPlace.get(s.get(consumed));
        consumed++;
        tensPlaceFound = true;
      }

      // we try adding the "and", and change our mind later if we don't find another number
      if (tensPlaceFound && consumed < s.size() && s.get(consumed).equals("and")) {
        consumed++;
      }
    }

    Conversion remaining = fromPrefix(s.subList(consumed, s.size()));
    int postfixNumDigits = Math.max(remaining.numDigits, minPostfixNumDigits);
    int num = prefix * ((int) Math.pow(10, postfixNumDigits)) + remaining.num;

    // if we saw "and" but no number came after it, then don't consume "and"
    if (s.get(consumed - 1).equals("and") && remaining.numTokens == 0) {
      consumed--;
    }
    return new Conversion(num, consumed + remaining.numTokens, prefixNumDigits + postfixNumDigits);
  }

  public String fromDigitsToText(String word) {
    // Maps "152" -> "one five two"
    return Arrays
      .asList(word.split(""))
      .stream()
      .map(digit -> digitToWord.get(digit))
      .collect(Collectors.joining(" "));
  }
}
