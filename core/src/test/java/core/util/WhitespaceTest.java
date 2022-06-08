package core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.BaseTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class WhitespaceTest extends BaseTest {

  private Whitespace whitespace = new Whitespace();

  @Test
  public void testFollowedByNewline() {
    String source = " abc xyz \n ";
    assertFalse(whitespace.followedByNewline(source, new Range(1, 4)));
    assertTrue(whitespace.followedByNewline(source, new Range(5, 8)));
  }

  @Test
  public void testLineNonWhitespaceStart() {
    String source = "f\n def foo";
    assertEquals(3, whitespace.lineNonWhitespaceStart(source, 9));
  }

  @Test
  public void testNextNewline() {
    String source = "f\n def foo";
    assertEquals(1, whitespace.nextNewline(source, 0));
    assertEquals(1, whitespace.nextNewline(source, 1));
    assertEquals(source.length(), whitespace.nextNewline(source, 2));
  }

  @Test
  public void testNonWhitespaceRanges() {
    String source = " abc xyz \n ";
    assertEquals(
      Arrays.asList(new Range(1, 4), new Range(5, 8)),
      whitespace.nonWhitespaceRanges(source)
    );
  }

  @Test
  public void testPreviousNewline() {
    String source = "aa\n def foo";
    assertEquals(-1, whitespace.previousNewline(source, 1));
    assertEquals(-1, whitespace.previousNewline(source, 2));
    assertEquals(2, whitespace.previousNewline(source, 5));
  }

  @Test
  public void testStripIndentation() {
    String source = ";\n  :def foo\n    xyz\n;\n";
    assertEquals(
      "def foo\n  xyz\n",
      whitespace.stripIndentation(source, new Range(5, source.length() - 2))
    );
  }

  @Test
  public void testStrip() {
    String source = " abc xyz \n ";
    assertEquals(new Range(1, 4), whitespace.leftStrip(source, new Range(0, 4)));
    assertEquals(new Range(5, 8), whitespace.rightStrip(source, new Range(5, 11)));
    assertEquals(new Range(1, 8), whitespace.strip(source, new Range(0, 11)));
  }

  @Test
  public void testIsWhitespace() {
    assertFalse(whitespace.isWhitespace('a'));
    assertTrue(whitespace.isWhitespace(' '));
    assertTrue(whitespace.isWhitespace('\t'));
    assertTrue(whitespace.isWhitespace('\r'));
    assertTrue(whitespace.isWhitespace('\n'));

    String source = " \tabc xyz \n ";
    assertFalse(whitespace.isWhitespace(source, new Range(0, 3)));
    assertTrue(whitespace.isWhitespace(source, new Range(0, 2)));
    assertTrue(whitespace.isWhitespace(source, new Range(9, 12)));
  }

  @Test
  public void testExpand() {
    String source = " abc xyz \n ";
    assertEquals(new Range(0, 4), whitespace.expandLeft(source, new Range(1, 4)));
    assertEquals(new Range(5, 11), whitespace.expandRight(source, new Range(5, 8)));
  }
}
