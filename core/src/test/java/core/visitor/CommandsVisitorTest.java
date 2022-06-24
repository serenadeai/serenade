package core.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.google.protobuf.ByteString;
import core.BaseServiceTest;
import core.gen.rpc.CallbackRequest;
import core.gen.rpc.CallbackType;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponse;
import core.gen.rpc.EditorState;
import core.gen.rpc.EvaluateRequest;
import core.gen.rpc.EvaluateTextRequest;
import core.gen.rpc.Language;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

// test commands that exist in every language but can't be captured in YAML tests
// we happen to use Python here, but none of this is Python-specific.
public class CommandsVisitorTest extends BaseServiceTest {

  private Language language = Language.LANGUAGE_PYTHON;

  private void clearHistory() {
    client.send(
      EvaluateRequest
        .newBuilder()
        .setCallbackRequest(
          CallbackRequest
            .newBuilder()
            .setType(CallbackType.CALLBACK_TYPE_CLEAR_HISTORY)
            .build()
        )
        .build()
    );
  }

  private void addToHistory(String command) {
    client.send(
      EvaluateRequest
        .newBuilder()
        .setCallbackRequest(
          CallbackRequest
            .newBuilder()
            .setType(CallbackType.CALLBACK_TYPE_ADD_TO_HISTORY)
            .setText(command)
            .build()
        )
        .build()
    );
  }

  @Test
  public void testAlternatives() {
    CommandsResponse response = makeRequest(
      "",
      0,
      Arrays.asList("copy line", "floopdy doop", "insert foo"),
      language
    );

    List<CommandType> types = response
      .getAlternativesList()
      .stream()
      .map(a -> a.getCommands(0).getType())
      .collect(Collectors.toList());

    assertEquals(types.size(), 3);
    assertEquals(
      new HashSet<>(types),
      new HashSet<>(
        Arrays.asList(
          CommandType.COMMAND_TYPE_COPY,
          CommandType.COMMAND_TYPE_INVALID,
          CommandType.COMMAND_TYPE_DIFF
        )
      )
    );

    // invalid commands are re-ordered.
    assertEquals(
      CommandType.COMMAND_TYPE_INVALID,
      response.getAlternatives(2).getCommands(0).getType()
    );
  }

  @Test
  public void testArrowKeyDirection() {
    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      makeRequest("", 0, "up", language)
        .getAlternatives(0)
        .getCommands(1)
        .getType()
    );

    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      makeRequest("", 0, "down", language)
        .getAlternatives(0)
        .getCommands(1)
        .getType()
    );

    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      makeRequest("", 0, "left", language)
        .getAlternatives(0)
        .getCommands(1)
        .getType()
    );

    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      makeRequest("", 0, "right", language)
        .getAlternatives(0)
        .getCommands(1)
        .getType()
    );

    assertEquals(
      2,
      makeRequest("", 0, "up two", language)
        .getAlternatives(0)
        .getCommands(1)
        .getIndex()
    );
  }

  @Test
  public void testBrokenChain() {
    assertEquals(
      "line one",
      makeRequest("", 0, "previous tab line one", language)
        .getExecute()
        .getRemaining()
    );
  }

  @Test
  public void testBrowserNavigation() {
    CommandsResponse r = makeRequest(
      EditorState
        .newBuilder()
        .setApplication("chrome")
        .setPluginInstalled(true)
        .setCursor(0)
        .setSource(ByteString.copyFromUtf8("one\ntwo\nthree"))
        .build(),
      Arrays.asList("go to google dot com")
    );
    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      r.getAlternatives(0).getCommands(0).getType()
    );
  }

  @Test
  public void testChain() {
    List<Command> r1 = makeRequest(
      "foo\nbar\n",
      0,
      "next line delete line",
      language
    )
      .getAlternatives(0)
      .getCommandsList();

    assertEquals(2, r1.size());

    assertEquals(CommandType.COMMAND_TYPE_DIFF, r1.get(0).getType());
    assertEquals("foo\nbar\n", r1.get(0).getSource());

    assertEquals(CommandType.COMMAND_TYPE_DIFF, r1.get(1).getType());
    assertEquals("foo\n", r1.get(1).getSource());

    List<Command> r2 = makeRequest("foo\nbar\n", 3, "newline indent", language)
      .getAlternatives(0)
      .getCommandsList();

    assertEquals(2, r2.size());

    assertEquals(CommandType.COMMAND_TYPE_DIFF, r2.get(0).getType());
    assertEquals("foo\n\nbar\n", r2.get(0).getSource());

    assertEquals(CommandType.COMMAND_TYPE_DIFF, r2.get(1).getType());
    assertEquals("foo\n    \nbar\n", r2.get(1).getSource());
  }

  @Test
  public void testChangeToSelf() {
    assertCommandType(
      "def foo():",
      4,
      "change foo to foo",
      language,
      CommandType.COMMAND_TYPE_INVALID
    );
  }

  @Test
  public void testCopy() {
    assertCommandType(
      "def foo():",
      4,
      "copy",
      language,
      CommandType.COMMAND_TYPE_PRESS
    );

    Command c1 = assertCommandType(
      "def foo():",
      4,
      "copy word",
      language,
      CommandType.COMMAND_TYPE_COPY
    );
    assertEquals("foo", c1.getText());

    // The following two tests depend on the second parse match,
    // since copy, phrase foo is preferred strongly by the model.
    Command c2 = assertCommandType(
      "def foo():",
      4,
      "copy phrase foo",
      language,
      CommandType.COMMAND_TYPE_COPY
    );
    assertEquals("foo", c2.getText());

    Command c3 = assertCommandType(
      "if True:",
      4,
      "copy phrase if",
      language,
      CommandType.COMMAND_TYPE_COPY
    );
    assertEquals("if", c3.getText());

    // we use copy commands here because you can't say "go to end of words 1 to 2"
    Command c4 = assertCommandType(
      "def foo bar",
      0,
      "copy words 1 to 2",
      language,
      CommandType.COMMAND_TYPE_COPY
    );
    assertEquals("def foo", c4.getText());

    Command c5 = assertCommandType(
      "foo ret",
      3,
      "copy next 3 characters",
      language,
      CommandType.COMMAND_TYPE_COPY
    );
    assertEquals("ret", c5.getText());
  }

  @Test
  public void testCrlf() {
    // note that we have to return new source to be consistent with the cursor when we remove \r.
    assertStringsMatch(
      fileAsString("function1_crlf.py"),
      73,
      "up one line",
      language,
      fileAsString("function1.py"),
      43,
      "",
      false
    );
  }

  @Test
  public void testCut() {
    assertCommandType(
      "def foo():",
      4,
      "cut",
      language,
      CommandType.COMMAND_TYPE_PRESS
    );

    List<Command> c1 = assertCommandTypes(
      "foo bar\n",
      0,
      "cut word",
      language,
      Arrays.asList(
        CommandType.COMMAND_TYPE_COPY,
        CommandType.COMMAND_TYPE_DIFF
      )
    );

    assertEquals("foo", c1.get(0).getText());
    assertEquals("bar\n", c1.get(1).getSource());
  }

  @Test
  public void testDictateMode() {
    assertCommandType(
      "",
      0,
      "dictate mode",
      language,
      CommandType.COMMAND_TYPE_START_DICTATE
    );
    assertCommandType(
      "",
      0,
      "stop dictating",
      language,
      CommandType.COMMAND_TYPE_STOP_DICTATE
    );
    EditorState nonPluginState = EditorState
      .newBuilder()
      .setApplication("")
      .setPluginInstalled(false)
      .setCursor(0)
      .setSource(ByteString.copyFromUtf8(""))
      .build();

    assertEquals(
      CommandType.COMMAND_TYPE_START_DICTATE,
      makeRequest(nonPluginState, Arrays.asList("dictate mode"))
        .getAlternatives(0)
        .getCommands(0)
        .getType()
    );

    assertEquals(
      CommandType.COMMAND_TYPE_STOP_DICTATE,
      makeRequest(nonPluginState, Arrays.asList("stop dictating"))
        .getAlternatives(0)
        .getCommands(0)
        .getType()
    );
  }

  @Test
  public void testDuplicates() {
    assertEquals(
      1,
      makeRequest(
        "def foo():\n    pass",
        0,
        Arrays.asList("add parameter foo", "add parameter foo"),
        language
      )
        .getAlternativesList()
        .size()
    );
  }

  @Test
  public void testFocus() {
    CommandsResponse r = makeRequest("", 0, "focus atom", language);
    assertEquals(
      CommandType.COMMAND_TYPE_FOCUS,
      r.getAlternatives(0).getCommands(0).getType()
    );
    assertEquals("atom", r.getAlternatives(0).getCommands(0).getText());
  }

  @Test
  public void testForwardDelete() {
    CommandsResponse r = makeRequest("", 0, "forward delete", language);
    assertEquals(1, r.getAlternatives(0).getCommandsList().size());
    assertEquals(
      "forwarddelete",
      r.getAlternatives(0).getCommands(0).getText()
    );
    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      r.getAlternatives(0).getCommands(0).getType()
    );
  }

  @Test
  public void testGoToWordChain() {
    CommandsResponse r = makeRequest(
      "foo bar baz",
      0,
      "next word paste",
      language
    );
    assertEquals(2, r.getAlternatives(0).getCommandsList().size());
  }

  @Test
  public void testSystem() {
    CommandsResponse r = makeRequest(
      EditorState.newBuilder().setApplication("chrome").build(),
      Arrays.asList("system hello world")
    );

    assertEquals(
      CommandType.COMMAND_TYPE_INSERT,
      r.getAlternatives(0).getCommands(0).getType()
    );
    assertEquals("hello world", r.getAlternatives(0).getCommands(0).getText());
  }

  @Test
  public void testLanguageModeCommand() {
    assertCommandType(
      "",
      0,
      "set language python",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );

    assertCommandType(
      "",
      0,
      "change language to python",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );

    assertCommandType(
      "",
      0,
      "language mode auto",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );

    assertCommandType(
      "",
      0,
      "language mode python",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );

    assertCommandType(
      "",
      0,
      "language mode c plus plus",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );

    assertCommandType(
      "",
      0,
      "language python",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );
    assertCommandType(
      "",
      0,
      "python mode",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );
    assertCommandType(
      "",
      0,
      "rust mode",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );
    assertCommandType(
      "",
      0,
      "c sharp mode",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );
    assertCommandType(
      "",
      0,
      "go mode",
      language,
      CommandType.COMMAND_TYPE_LANGUAGE_MODE
    );
  }

  @Test
  public void testLaunch() {
    CommandsResponse r = makeRequest("", 0, "launch atom", language);
    assertEquals(
      CommandType.COMMAND_TYPE_LAUNCH,
      r.getAlternatives(0).getCommands(0).getType()
    );
    assertEquals("atom", r.getAlternatives(0).getCommands(0).getText());
  }

  public void testInspect() {
    assertCommandType(
      "def foo(a):",
      8,
      "inspect",
      language,
      CommandType.COMMAND_TYPE_DEBUGGER_SHOW_HOVER
    );

    List<Command> c = assertCommandTypes(
      "def foo(a):\n   pass\n",
      0,
      "inspect parameter",
      language,
      Arrays.asList(
        CommandType.COMMAND_TYPE_DIFF,
        CommandType.COMMAND_TYPE_DEBUGGER_SHOW_HOVER
      )
    );

    assertEquals(8, c.get(0).getCursor());

    c =
      assertCommandTypes(
        "def foo(a):\n   pass\n",
        0,
        "show value of a",
        language,
        Arrays.asList(
          CommandType.COMMAND_TYPE_DIFF,
          CommandType.COMMAND_TYPE_DEBUGGER_SHOW_HOVER
        )
      );

    assertEquals(8, c.get(0).getCursor());
  }

  @Test
  public void testNativeDiff() {
    CommandsResponse r1 = makeRequest(
      "",
      0,
      Arrays.asList("type hello"),
      language
    );
    assertEquals(
      1,
      r1.getAlternatives(0).getCommands(0).getChangesList().size()
    );
    assertEquals(
      CommandType.COMMAND_TYPE_DIFF,
      r1.getAlternatives(0).getCommands(0).getType()
    );
    assertEquals(5, r1.getAlternatives(0).getCommands(0).getCursor());
    assertEquals("hello", r1.getAlternatives(0).getCommands(0).getSource());
    assertEquals(
      "hello",
      r1.getAlternatives(0).getCommands(0).getChanges(0).getSubstitution()
    );
    assertEquals(
      0,
      r1.getAlternatives(0).getCommands(0).getChanges(0).getStart()
    );
    assertEquals(
      0,
      r1.getAlternatives(0).getCommands(0).getChanges(0).getStop()
    );

    CommandsResponse r2 = makeRequest(
      "today is is tuesday",
      0,
      Arrays.asList("delete second word"),
      language
    );

    assertEquals(
      CommandType.COMMAND_TYPE_DIFF,
      r2.getAlternatives(0).getCommands(0).getType()
    );
    assertEquals(6, r2.getAlternatives(0).getCommands(0).getCursor());
    assertEquals(
      "today is tuesday",
      r2.getAlternatives(0).getCommands(0).getSource()
    );
    assertEquals(
      "",
      r2.getAlternatives(0).getCommands(0).getChanges(0).getSubstitution()
    );
    assertEquals(
      6,
      r2.getAlternatives(0).getCommands(0).getChanges(0).getStart()
    );
    assertEquals(
      9,
      r2.getAlternatives(0).getCommands(0).getChanges(0).getStop()
    );

    CommandsResponse r3 = makeRequest(
      "one\ntwo\nthree",
      0,
      Arrays.asList("end of second line"),
      language
    );

    assertEquals(
      CommandType.COMMAND_TYPE_DIFF,
      r3.getAlternatives(0).getCommands(0).getType()
    );
    assertEquals(7, r3.getAlternatives(0).getCommands(0).getCursor());
  }

  @Test
  public void testNumbersInNonTextCommand() {
    String source = "\n".repeat(200);
    Command c1 = assertCommandType(
      source,
      0,
      "line a hundred",
      language,
      CommandType.COMMAND_TYPE_DIFF
    );
    assertEquals(99, c1.getCursor());

    Command c2 = assertCommandType(
      source,
      0,
      "line one hundred and two",
      language,
      CommandType.COMMAND_TYPE_DIFF
    );
    assertEquals(101, c2.getCursor());

    Command c3 = assertCommandType(
      source,
      0,
      "line one hundred and twenty",
      language,
      CommandType.COMMAND_TYPE_DIFF
    );
    assertEquals(119, c3.getCursor());
  }

  @Test
  public void testOpenUrl() {
    CommandsResponse r = makeRequest(
      EditorState
        .newBuilder()
        .setApplication("chrome")
        .setPluginInstalled(true)
        .setCursor(0)
        .setSource(ByteString.copyFromUtf8(""))
        .build(),
      Arrays.asList("open stackoverflow.com", "open stackoverflow.rom")
    );

    assertEquals(2, r.getAlternativesList().size());
    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      r.getAlternatives(0).getCommands(0).getType()
    );
  }

  @Test
  public void testPasteDirection() {
    Command c1 = assertCommandType(
      " ",
      0,
      "paste below",
      language,
      CommandType.COMMAND_TYPE_CLIPBOARD
    );
    assertEquals(c1.getDirection(), "below");

    Command c2 = assertCommandType(
      " ",
      0,
      "paste above",
      language,
      CommandType.COMMAND_TYPE_CLIPBOARD
    );
    assertEquals(c2.getDirection(), "above");
  }

  @Test
  public void testPasteWithoutPlugin() {
    CommandsResponse r = makeRequest(
      EditorState.newBuilder().setApplication("textedit").build(),
      Arrays.asList("paste")
    );

    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      r.getAlternatives(0).getCommands(0).getType()
    );
  }

  @Test
  public void testPressQuantifier() {
    CommandsResponse r2 = makeRequest("", 0, "press up two times", language);
    assertEquals(2, r2.getAlternatives(0).getCommandsList().size());
    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      r2.getAlternatives(0).getCommands(0).getType()
    );
  }

  @Test
  public void testQuantifier() {
    CommandsResponse response = makeRequest(
      "foo\nbar",
      0,
      "delete line two times",
      language
    );
    assertEquals(
      CommandType.COMMAND_TYPE_DIFF,
      response.getAlternatives(0).getCommands(0).getType()
    );

    assertEquals(
      CommandType.COMMAND_TYPE_DIFF,
      response.getAlternatives(0).getCommands(1).getType()
    );
  }

  @Test
  public void testRepeat() {
    clearHistory();
    addToHistory("new tab");
    assertCommandType(
      "",
      0,
      "repeat",
      language,
      CommandType.COMMAND_TYPE_CREATE_TAB
    );

    clearHistory();
    addToHistory("new tab");
    addToHistory("close tab");
    assertCommandType(
      "",
      0,
      "repeat",
      language,
      CommandType.COMMAND_TYPE_CLOSE_TAB
    );
    assertCommandType(
      "",
      0,
      "repeat new",
      language,
      CommandType.COMMAND_TYPE_CREATE_TAB
    );
    assertCommandType(
      "",
      0,
      "repeat close",
      language,
      CommandType.COMMAND_TYPE_CLOSE_TAB
    );
    assertCommandType(
      "",
      0,
      "repeat",
      language,
      CommandType.COMMAND_TYPE_CLOSE_TAB
    );

    // test we don't loop infinitely.
    clearHistory();
    addToHistory("change word to bar");
    addToHistory("end of line repeat change");
    assertCommandTypes(
      "foo\n",
      1,
      "end of line repeat change",
      language,
      Arrays.asList(
        CommandType.COMMAND_TYPE_DIFF,
        CommandType.COMMAND_TYPE_DIFF
      )
    );
  }

  @Test
  public void testRepeatQuantifier() {
    clearHistory();
    addToHistory("delete character");

    assertCommandTypes(
      "abcdefghijklmn\n",
      3,
      "repeat twice",
      language,
      Arrays.asList(
        CommandType.COMMAND_TYPE_DIFF,
        CommandType.COMMAND_TYPE_DIFF
      )
    );

    assertCommandTypes(
      "abcdefghijklmn\n",
      3,
      "repeat delete twice",
      language,
      Arrays.asList(
        CommandType.COMMAND_TYPE_DIFF,
        CommandType.COMMAND_TYPE_DIFF
      )
    );

    assertCommandTypes(
      "abcdefghijklmn\n",
      8,
      "repeat five times",
      language,
      Arrays.asList(
        CommandType.COMMAND_TYPE_DIFF,
        CommandType.COMMAND_TYPE_DIFF,
        CommandType.COMMAND_TYPE_DIFF,
        CommandType.COMMAND_TYPE_DIFF,
        CommandType.COMMAND_TYPE_DIFF
      )
    );
  }

  @Test
  public void testRevisionBox() {
    CommandsResponse r = makeRequest(
      EditorState
        .newBuilder()
        .setClientIdentifier("1.10.0")
        .setCursor(0)
        .setSource(ByteString.copyFromUtf8("one\ntwo\nthree"))
        .build(),
      Arrays.asList("revise")
    );
    assertEquals(
      CommandType.COMMAND_TYPE_SHOW_REVISION_BOX,
      r.getAlternatives(0).getCommands(0).getType()
    );
    r =
      makeRequest(
        EditorState
          .newBuilder()
          .setApplication("revision-box")
          .setClientIdentifier("1.10.0")
          .setCursor(0)
          .setSource(ByteString.copyFromUtf8("one\ntwo\nthree"))
          .build(),
        Arrays.asList("send")
      );
    assertEquals(
      CommandType.COMMAND_TYPE_HIDE_REVISION_BOX,
      r.getAlternatives(0).getCommands(0).getType()
    );
  }

  @Test
  public void testSwitchWindow() {
    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      makeRequest("", 0, "switch windows", language)
        .getAlternatives(0)
        .getCommands(0)
        .getType()
    );
    assertEquals(
      CommandType.COMMAND_TYPE_PRESS,
      makeRequest("", 0, "switch window", language)
        .getAlternatives(0)
        .getCommands(0)
        .getType()
    );
  }

  @Test
  public void testTabs() {
    Command c1 = assertCommandType(
      "",
      0,
      "go to tab 2",
      language,
      CommandType.COMMAND_TYPE_SWITCH_TAB
    );
    assertEquals(2, c1.getIndex());

    Command c2 = assertCommandType(
      "",
      0,
      "go to fourth tab",
      language,
      CommandType.COMMAND_TYPE_SWITCH_TAB
    );
    assertEquals(4, c2.getIndex());

    Command c3 = assertCommandType(
      "",
      0,
      "Tab 3",
      language,
      CommandType.COMMAND_TYPE_SWITCH_TAB
    );
    assertEquals(3, c3.getIndex());
  }

  @Test
  public void testUnsupportedFile() {
    assertEquals(
      CommandType.COMMAND_TYPE_INVALID,
      makeRequest("hello there", 0, Arrays.asList("next type"), "file.txt")
        .getAlternatives(0)
        .getCommands(0)
        .getType()
    );
  }

  @Test
  public void testUnicode() {
    assertStringsMatch(
      fileAsString("unicode.js"),
      161,
      "add function foo",
      "unicode.js",
      fileAsString("unicode_add.js"),
      227,
      "",
      false
    );
  }

  @Test
  public void testUse() {
    assertCommandType("", 0, "use one", language, CommandType.COMMAND_TYPE_USE);

    // Disable reranking so that we can check invalid ordering.
    CommandsResponse response = makeRequest(
      editorState("foo\n", 0, language),
      EvaluateTextRequest
        .newBuilder()
        .setText("copy line ; use two ; type hello")
        .setIncludeAlternatives(true)
        .setRerank(false)
        .build()
    );

    // copy line
    assertNotEquals(
      CommandType.COMMAND_TYPE_INVALID,
      response.getAlternatives(0).getCommands(0).getType()
    );

    // copy, line or copy "line", comes next, but don't bother checking
    // it's not pruned because reranking is disabled

    // use two
    assertEquals(
      CommandType.COMMAND_TYPE_INVALID,
      response
        .getAlternatives(response.getAlternativesList().size() - 2)
        .getCommands(0)
        .getType()
    );

    // type hello
    assertNotEquals(
      CommandType.COMMAND_TYPE_INVALID,
      response
        .getAlternatives(response.getAlternativesList().size() - 1)
        .getCommands(0)
        .getType()
    );
  }
}
