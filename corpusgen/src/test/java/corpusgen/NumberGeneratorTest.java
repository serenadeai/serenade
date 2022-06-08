package corpusgen.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import core.util.NumberConverter;
import corpusgen.CorpusGenComponent;
import corpusgen.DaggerCorpusGenComponent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NumberGeneratorTest {

  private void assertGenerates(String number, List<String> expected) {
    NumberGenerator generator = DaggerCorpusGenComponent.create().numberGenerator();
    NumberConverter numberConverter = DaggerCorpusGenComponent.create().numberConverter();
    Set<String> actual = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      String generatedNumber = generator.sampleEnglish(number);
      assertTrue(
        numberConverter.isValid(generatedNumber),
        "Sampling: [" + number + "] but invalid representation: " + generatedNumber
      );
      actual.add(generatedNumber);
    }
    for (String expectedEnglish : expected) {
      assertTrue(
        actual.contains(expectedEnglish),
        expectedEnglish + " isn't contained in generated" + actual
      );
    }
  }

  @Test
  public void test() {
    assertGenerates("1000", Arrays.asList("a thousand", "one thousand"));
    assertGenerates("100", Arrays.asList("a hundred", "one hundred"));
    assertGenerates(
      "105",
      Arrays.asList("a hundred and five", "one hundred and five", "one o five")
    );
    assertGenerates("12", Arrays.asList("twelve"));
    assertGenerates("52", Arrays.asList("fifty two"));
    assertGenerates("123", Arrays.asList("one two three", "one twenty three"));
    assertGenerates(
      "100001",
      Arrays.asList("a hundred thousand and one", "one hundred thousand one")
    );
    assertGenerates("0", Arrays.asList("zero", "o"));
    assertGenerates("0012", Arrays.asList("zero zero twelve", "o o twelve"));
    assertGenerates(
      "9154",
      Arrays.asList(
        "nine thousand one hundred fifty four",
        "nine thousand and one hundred fifty four"
      )
    );
    assertGenerates("8111", Arrays.asList("eighty one hundred eleven"));
  }
}
