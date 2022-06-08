package core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import core.BaseTest;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

public class TextStylerTest extends BaseTest {

  protected TextStyler textStyler = new TextStyler();

  @Test
  public void testGetStyle() {
    assertEquals(
      new HashSet<TextStyle>(Arrays.asList(TextStyle.UNKNOWN)),
      textStyler.getStyle("Hi There")
    );
    assertEquals(
      new HashSet<TextStyle>(Arrays.asList(TextStyle.LOWERCASE)),
      textStyler.getStyle("hi there")
    );
    assertEquals(
      new HashSet<TextStyle>(Arrays.asList(TextStyle.CAMEL_CASE)),
      textStyler.getStyle("hiThere")
    );
    assertEquals(
      new HashSet<TextStyle>(Arrays.asList(TextStyle.PASCAL_CASE)),
      textStyler.getStyle("HiThere")
    );
    assertEquals(
      new HashSet<TextStyle>(Arrays.asList(TextStyle.DASHES)),
      textStyler.getStyle("hi-there")
    );
    assertEquals(
      new HashSet<TextStyle>(Arrays.asList(TextStyle.UNDERSCORES)),
      textStyler.getStyle("hi_there")
    );
    assertEquals(
      new HashSet<TextStyle>(Arrays.asList(TextStyle.ALL_CAPS)),
      textStyler.getStyle("HI_THERE")
    );
  }

  @Test
  public void testStyle() {
    assertEquals("FOO_BAR", textStyler.style("foo bar", TextStyle.ALL_CAPS));
    assertEquals("Foo bar", textStyler.style("foo bar", TextStyle.CAPITALIZED));
    assertEquals("fooBar", textStyler.style("foo bar", TextStyle.CAMEL_CASE));
    assertEquals("foo-bar", textStyler.style("foo bar", TextStyle.DASHES));
    assertEquals("FooBar", textStyler.style("foo bar", TextStyle.PASCAL_CASE));
    assertEquals("foo_bar", textStyler.style("foo bar", TextStyle.UNDERSCORES));
  }

  @Test
  public void testAllCaps() {
    assertEquals("FOO_BAR_BAZ", textStyler.toAllCaps("foo bar baz"));
  }

  @Test
  public void testToCapitalized() {
    assertEquals("Foo bar baz qux", textStyler.toCapitalized("foo bar baz qux"));
  }

  @Test
  public void testToCamelCase() {
    assertEquals("fooBarBazQux", textStyler.toCamelCase("foo bar baz qux"));
  }

  @Test
  public void testToDashes() {
    assertEquals("foo-bar-baz-qux", textStyler.toDashes("foo bar baz qux"));
  }

  @Test
  public void testToPascalCase() {
    assertEquals("FooBarBazQux", textStyler.toPascalCase("foo bar baz qux"));
  }

  @Test
  public void testToUnderscores() {
    assertEquals("foo_bar_baz_qux", textStyler.toUnderscores("foo bar baz qux"));
  }
}
