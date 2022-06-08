package corpusgen.util;

import core.util.NumberConverter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NumberGenerator {

  private Map<Integer, String> tensPlaceNames;
  private Map<String, String> digitNames;
  private Map<String, String> teenNames;
  private Map<String, String> tenNames;
  private NumberConverter numberConverter;

  @Inject
  public NumberGenerator(NumberConverter numberConverter) {
    this.numberConverter = numberConverter;
    tensPlaceNames =
      numberConverter.tensPlace
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> e.getValue(), e -> e.getKey()));
    digitNames =
      numberConverter.digits
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> Integer.toString(e.getValue()), e -> e.getKey()));
    tenNames =
      numberConverter.tens
        .entrySet()
        .stream()
        .collect(
          Collectors.toMap(e -> Integer.toString(e.getValue()).substring(0, 1), e -> e.getKey())
        );
    teenNames =
      numberConverter.teens
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> Integer.toString(e.getValue()), e -> e.getKey()));
  }

  public String sampleOneAlternative(String number) {
    if (number.equals("one") && ThreadLocalRandom.current().nextDouble() < 0.25) {
      return "a";
    }
    return number;
  }

  public String sampleEnglish(int number) {
    return sampleEnglish(Integer.toString(number));
  }

  public String sampleEnglish(String number) {
    if (ThreadLocalRandom.current().nextDouble() < 0.1) {
      String ret = "";
      for (int i = 0; i < number.length(); i++) {
        ret += " " + digitNames.get(Character.toString(number.charAt(i)));
      }
      return ret.substring(1);
    }

    String ret = "";
    String zero = ThreadLocalRandom.current().nextBoolean() ? "zero" : "o";
    while (number.startsWith("0")) {
      ret += zero + " ";
      number = number.substring(1);
    }
    ret += sampleComposite(number, false);
    if (ret.startsWith("and ")) {
      ret = ret.substring("and ".length());
    }
    ret = ret.replaceAll("\\s+", " ").trim();

    return ret;
  }

  public String sampleComposite(String number) {
    return sampleComposite(number, true);
  }

  public String sampleComposite(String number, boolean sampleAndAllowed) {
    if (number.equals("")) {
      return "";
    } else if (number.startsWith("0")) {
      return sampleComposite(number.substring(1));
    }

    if (
      number.length() == 3 || (number.length() == 4 && ThreadLocalRandom.current().nextBoolean())
    ) {
      if (
        number.charAt(number.length() - 2) == '0' &&
        number.charAt(number.length() - 3) != '0' &&
        number.charAt(number.length() - 1) != '0' &&
        ThreadLocalRandom.current().nextBoolean()
      ) {
        // e.g. one o five
        return (
          sampleComposite(number.substring(0, number.length() - 2)) +
          " o " +
          sampleComposite(number.substring(number.length() - 1), false)
        );
      }
      if (number.charAt(number.length() - 2) != '0' && ThreadLocalRandom.current().nextBoolean()) {
        // e.g one twenty three, twelve twenty three
        return (
          sampleComposite(number.substring(0, number.length() - 2)) +
          " " +
          sampleComposite(number.substring(number.length() - 2), false)
        );
      }
      // e.g. one hundred twenty five, twelve hundred twenty five
      return (
        sampleOneAlternative(sampleComposite(number.substring(0, number.length() - 2))) +
        " hundred " +
        sampleComposite(number.substring(number.length() - 2))
      );
    } else if (number.length() >= 4) {
      int minDigits = ((number.length() - 1) / 3) * 3;
      int splitIndex = number.length() - minDigits;
      return (
        sampleOneAlternative(sampleComposite(number.substring(0, splitIndex))) +
        " " +
        tensPlaceNames.get(minDigits) +
        " " +
        sampleComposite(number.substring(splitIndex))
      );
    }
    String ret = "";
    if (sampleAndAllowed && ThreadLocalRandom.current().nextBoolean()) {
      ret = "and ";
    }
    if (number.length() == 2 && tenNames.containsKey(number.substring(0, 1))) {
      // 20 -> 99
      return number.substring(1).equals("0")
        ? tenNames.get(number.substring(0, 1))
        : tenNames.get(number.substring(0, 1)) + " " + digitNames.get(number.substring(1));
    }
    if (teenNames.containsKey(number)) {
      // 10 -> 19
      ret += teenNames.get(number);
    } else if (digitNames.containsKey(number)) {
      // 0 -> 9
      ret += digitNames.get(number);
    }
    return ret;
  }
}
