package core.codeengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.BaseTest;
import core.gen.rpc.Language;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class TokenizerTest extends BaseTest {

  private void assertTokenizedMatches(String code, String expected) {
    Tokenizer tokenizer = component.tokenizer();
    String output = tokenizer
      .tokenize(code)
      .stream()
      .map(t -> t.modelCodeRepresentation())
      .collect(Collectors.joining(" "));
    assertEquals(expected, output);
  }

  @Test
  public void test() {
    assertTokenizedMatches("    Python3Ast 341", "I C python 3 C ast SP 3 4 1");
    assertTokenizedMatches(
      "    List<List<String>> nameLists = new ArrayList",
      "I C list < C list < C string > > SP name C lists SP = SP new SP C array C list"
    );
    assertTokenizedMatches("foo[BAZ_BOO]", "NL foo [ A baz _ A boo ]");
    assertTokenizedMatches("IOException", "NL A io C exception");
    assertTokenizedMatches("FOO_BAR_ALONE", "NL A foo _ A bar _ A alone");
  }

  @Test
  public void testContractions() {
    assertTokenizedMatches("can't", "NL can't");
    assertTokenizedMatches("Can't", "NL C can't");
  }
}
