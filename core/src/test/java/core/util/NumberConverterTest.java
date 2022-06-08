package core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.BaseTest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class NumberConverterTest extends BaseTest {

  private NumberConverter numberConverter = new NumberConverter();

  @Test
  public void testConvertNumbers() {
    assertEquals(
      "equals one hundred",
      numberConverter.convertNumbers("equals the word one hundred")
    );
    assertEquals(
      "o 14 the letter o shouldn't be converted but 0 should",
      numberConverter.convertNumbers(
        "o fourteen the letter o shouldn't be converted but zero should"
      )
    );
    assertEquals("one two three", numberConverter.convertNumbers("escape one two three"));
    assertEquals("equals 100", numberConverter.convertNumbers("equals one hundred"));
    assertEquals("equals 100", numberConverter.convertNumbers("equals a hundred"));
    assertEquals("equals 1000", numberConverter.convertNumbers("equals a thousand"));
    assertEquals("equals 1012", numberConverter.convertNumbers("equals a thousand and twelve"));
    assertEquals("equals 1001", numberConverter.convertNumbers("equals one thousand and one"));
    assertEquals("a and b", numberConverter.convertNumbers("a and b"));
    assertEquals("x and y", numberConverter.convertNumbers("x and y"));
    assertEquals(
      "go to line 3025",
      numberConverter.convertNumbers("go to line thirty twenty five")
    );
    assertEquals(
      "localhost colon 8080",
      numberConverter.convertNumbers("localhost colon eighty eighty")
    );
  }

  @Test
  public void testDigitSequence() {
    List<String> digitWords = Arrays.asList(
      "zero",
      "one",
      "two",
      "three",
      "four",
      "five",
      "six",
      "seven",
      "eight",
      "nine"
    );
    for (int number = 1; number <= 10000; number++) {
      String numberString = String
        .valueOf(number)
        .chars()
        .mapToObj(c -> digitWords.get(Character.getNumericValue((char) c)))
        .collect(Collectors.joining(" "));
      assertEquals(
        "go to line " + String.valueOf(number),
        numberConverter.convertNumbers("go to line " + numberString)
      );
      assertEquals(number, numberConverter.fromString(numberString));
    }
  }

  @Test
  public void testFromString() {
    assertEquals(0, numberConverter.fromString("zero"));
    assertEquals(100, numberConverter.fromString("one hundred"));
    assertEquals(102, numberConverter.fromString("one o two"));
    assertEquals(20012, numberConverter.fromString("twenty thousand twelve"));
    assertEquals(1204, numberConverter.fromString("twelve o four"));
    assertEquals(1215, numberConverter.fromString("twelve fifteen"));
    assertEquals(99, numberConverter.fromString("ninety nine"));
    assertEquals(1902, numberConverter.fromString("nineteen o two"));
    assertEquals(2016, numberConverter.fromString("twenty sixteen"));
    assertEquals(321, numberConverter.fromString("three two one"));
  }

  @Test
  public void testIsValid() {
    assertTrue(numberConverter.isValid("zero"));
    assertFalse(numberConverter.isValid("zero and"));
  }
}
