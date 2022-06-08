package core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import core.BaseTest;
import core.evaluator.TranscriptNormalizer;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import speechengine.gen.rpc.Alternative;

public class TranscriptNormalizerTest extends BaseTest {

  private static TranscriptNormalizer transcriptNormalizer;

  @BeforeAll
  public static void setup() {
    transcriptNormalizer = component.transcriptNormalizer();
  }

  private String normalize(String s) {
    return transcriptNormalizer
      .normalize(Arrays.asList(Alternative.newBuilder().setTranscript(s).build()))
      .get(0)
      .getTranscript();
  }

  @Test
  public void testSymbols() {
    assertEquals(
      normalize("there's no'thi'ng to' 'convert here"),
      "there's no'thi'ng to' 'convert here"
    );
    assertEquals(normalize("something **-"), "something star star dash");
  }

  @Test
  public void testNumbers() {
    assertEquals(normalize("r2d2"), "r two d two");
  }
}
