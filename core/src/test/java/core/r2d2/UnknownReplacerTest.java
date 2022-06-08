package core.codeengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.BaseTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

public class UnknownReplacerTest extends BaseTest {

  @Test
  public void testMultipleStringsWithUnknownsAutoStyle() {
    List<String> testStrings = new ArrayList<>(Arrays.asList("foo bar subparsersaaa", "foo bar"));
    UnknownReplacer unknownReplacer = new UnknownReplacer(
      new HashSet<>(Arrays.asList("foo", "bar"))
    );
    UnknownReplacer.StringsWithUnknowns s = unknownReplacer.stringsWithUnknowns(testStrings);
    assertTrue(s.strings.get(0).matches("foo bar UNK[0-9]+"), "Couldn't match " + s.strings.get(0));
    assertEquals(
      s.strings.get(0),
      unknownReplacer.replaceUnknowns("foo bar subparsersaaa", s.unknowns)
    );
    assertEquals(
      "foo bar subparsersaaa",
      unknownReplacer.resolveUnknowns(s.strings.get(0), s.unknowns)
    );
    assertTrue(s.strings.get(1).matches("foo bar"), "Couldn't match " + s.strings.get(1));
    assertEquals("foo bar", unknownReplacer.resolveUnknowns(s.strings.get(1), s.unknowns));
  }
}
