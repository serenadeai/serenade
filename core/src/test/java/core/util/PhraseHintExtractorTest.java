package core.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PhraseHintExtractorTest {

  @Test
  public void testGenerate() {
    PhraseHintExtractor phraseHintExtractor = new PhraseHintExtractor();
    String source = "MAX_SOMETHING minSomething\n HelloThere\nbar";
    List<String> hints = Arrays.asList("PIKAchu", "pikachu", "char mander");
    List<String> phraseHints = phraseHintExtractor.extract(source, hints);

    // contains the above alphanumerics with the styles removed
    assertTrue(phraseHints.contains("max"));
    assertTrue(phraseHints.contains("something"));
    assertTrue(phraseHints.contains("min"));
    assertTrue(phraseHints.contains("hello"));
    assertTrue(phraseHints.contains("there"));
    assertTrue(phraseHints.contains("bar"));
    assertTrue(phraseHints.contains("pikachu"));
    assertTrue(phraseHints.contains("charmander"));
  }
}
