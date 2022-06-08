package core.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import core.BaseTest;
import core.evaluator.ParsedTranscript;
import core.evaluator.TranscriptParser;
import core.gen.antlr.command.CommandLexer;
import core.gen.antlr.command.CommandParser;
import core.gen.rpc.Language;
import core.parser.CommandAntlrParser;
import core.util.ObjectType;
import core.util.Range;
import core.util.SearchDirection;
import core.util.selection.Selection;
import core.util.selection.SelectionEndpoint;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import speechengine.gen.rpc.Alternative;

public class TreeConverterTest extends BaseTest {

  private Language language = Language.LANGUAGE_PYTHON;

  protected CommonTokenStream tokens(String input) {
    CharStream stream = CharStreams.fromString(input);
    CommandLexer lexer = new CommandLexer(stream);
    return new CommonTokenStream(lexer);
  }

  protected CommandParser parser(CommonTokenStream tokens) {
    return new CommandParser(tokens);
  }

  protected Selection convertSelection(String input) {
    String transcript = "select " + input;
    CommonTokenStream tokens = tokens(transcript);
    CommandParser parser = parser(tokens);
    CommandAntlrParser commandAntlrParser = component.commandAntlrParser();
    ParsedTranscript parsed = new ParsedTranscript(
      Alternative.newBuilder().setTranscript(transcript).build(),
      true,
      commandAntlrParser.convertAntlrTree(transcript, tokens, parser.main())
    );

    TreeConverter TreeConverter = component.treeConverter();
    return TreeConverter.convertSelectionWithImplicitPhrase(
      parsed
        .getCommandChain()
        .getChild("command")
        .get()
        .getChild("select")
        .get()
        .getChild("selectionWithImplicitPhrase")
        .get()
    );
  }

  protected Integer convertNumber(String input) {
    // Since this needs to be part of a command, we use go to line numbers to parse these numbers.

    String transcript = "line " + input;
    CommonTokenStream tokens = tokens(transcript);
    CommandParser parser = parser(tokens);
    CommandAntlrParser commandAntlrParser = component.commandAntlrParser();
    ParsedTranscript parsed = new ParsedTranscript(
      Alternative.newBuilder().setTranscript(transcript).build(),
      true,
      commandAntlrParser.convertAntlrTree(transcript, tokens, parser.main())
    );
    TreeConverter TreeConverter = component.treeConverter();

    Integer number = TreeConverter.convertNumber(
      parsed
        .getCommandChain()
        .getChild("command")
        .get()
        .getChild("goTo")
        .get()
        .getChild("positionSelection")
        .get()
        .getChild("unnamedPositionSelection")
        .get()
        .getChild("number")
        .get()
    );

    return number;
  }

  @Test
  public void testNodeDescriptor() {
    Object[][] args = new Object[][] {
      { "argument", ObjectType.ARGUMENT },
      { "argument list", ObjectType.ARGUMENT_LIST },
      { "assert", ObjectType.ASSERT },
      { "assignment", ObjectType.ASSIGNMENT },
      { "assignment value", ObjectType.ASSIGNMENT_VAL },
      { "assignment variable", ObjectType.ASSIGNMENT_VARIABLE },
      { "body", ObjectType.BODY },
      { "class", ObjectType.CLASS },
      { "condition", ObjectType.CONDITION },
      { "decorator", ObjectType.DECORATOR },
      { "dictionary", ObjectType.DICTIONARY },
      { "else", ObjectType.ELSE },
      { "for", ObjectType.FOR },
      { "function", ObjectType.FUNCTION },
      { "generator", ObjectType.GENERATOR },
      { "if", ObjectType.IF },
      { "import", ObjectType.IMPORT },
      { "keyword parameter", ObjectType.KEYWORD_PARAMETER },
      { "lambda", ObjectType.LAMBDA },
      { "list", ObjectType.LIST },
      { "method", ObjectType.METHOD },
      { "parameter", ObjectType.PARAMETER },
      { "parameter list", ObjectType.PARAMETER_LIST },
      { "property", ObjectType.PROPERTY },
      { "return", ObjectType.RETURN },
      { "return value", ObjectType.RETURN_VAL },
      { "set", ObjectType.SET },
      { "statement", ObjectType.STATEMENT },
      { "string", ObjectType.STRING },
      { "tuple", ObjectType.TUPLE },
      { "with alias", ObjectType.WITH_ALIAS },
      { "with item", ObjectType.WITH_ITEM },
      { "while", ObjectType.WHILE },
    };

    for (Object[] arg : args) {
      assertEquals(arg[1], convertSelection((String) arg[0]).object);
    }
  }

  @Test
  public void testNumber() {
    assertEquals(0, (int) convertNumber("zero"));

    assertEquals(1, (int) convertNumber("one"));

    assertEquals(50, (int) convertNumber("fifty"));

    assertEquals(51, (int) convertNumber("fifty one"));
  }

  @Test
  public void testSelection() {
    Object[][] args = new Object[][] {
      { "block", new Selection.Builder(ObjectType.BLOCK) },
      { "character", new Selection.Builder(ObjectType.CHARACTER) },
      { "file", new Selection.Builder(ObjectType.FILE) },
      { "term", new Selection.Builder(ObjectType.TERM) },
      { "word", new Selection.Builder(ObjectType.WORD) },
      { "phrase capital foo", new Selection.Builder(ObjectType.PHRASE).setName("capital foo") },
      { "phrase foo", new Selection.Builder(ObjectType.PHRASE).setName("foo") },
      { "end of line", new Selection.Builder(ObjectType.LINE).setEndpoint(SelectionEndpoint.END) },
      { "end of word", new Selection.Builder(ObjectType.WORD).setEndpoint(SelectionEndpoint.END) },
      {
        "end of phrase foo",
        new Selection.Builder(ObjectType.PHRASE).setEndpoint(SelectionEndpoint.END).setName("foo"),
      },
      { "line 3", new Selection.Builder(ObjectType.LINE).setAbsoluteRange(new Range(2)) },
      { "line three", new Selection.Builder(ObjectType.LINE).setAbsoluteRange(new Range(2)) },
      {
        "line four o three",
        new Selection.Builder(ObjectType.LINE).setAbsoluteRange(new Range(402)),
      },
      {
        "line thirteen o three",
        new Selection.Builder(ObjectType.LINE).setAbsoluteRange(new Range(1302)),
      },
      {
        "line two thousand one hundred ninety two",
        new Selection.Builder(ObjectType.LINE).setAbsoluteRange(new Range(2191)),
      },
      {
        "line four twenty three",
        new Selection.Builder(ObjectType.LINE).setAbsoluteRange(new Range(422)),
      },
      {
        "next character",
        new Selection.Builder(ObjectType.CHARACTER).setDirection(SearchDirection.NEXT),
      },
      { "next line", new Selection.Builder(ObjectType.LINE).setDirection(SearchDirection.NEXT) },
      { "next word", new Selection.Builder(ObjectType.WORD).setDirection(SearchDirection.NEXT) },
      {
        "next phrase foo",
        new Selection.Builder(ObjectType.PHRASE).setDirection(SearchDirection.NEXT).setName("foo"),
      },
      {
        "next 2 words",
        new Selection.Builder(ObjectType.WORD).setDirection(SearchDirection.NEXT).setCount(2),
      },
      {
        "next two words",
        new Selection.Builder(ObjectType.WORD).setDirection(SearchDirection.NEXT).setCount(2),
      },
      { "2 words", new Selection.Builder(ObjectType.WORD).setCount(2) },
      {
        "previous character",
        new Selection.Builder(ObjectType.CHARACTER).setDirection(SearchDirection.PREVIOUS),
      },
      {
        "previous line",
        new Selection.Builder(ObjectType.LINE).setDirection(SearchDirection.PREVIOUS),
      },
      {
        "previous word",
        new Selection.Builder(ObjectType.WORD).setDirection(SearchDirection.PREVIOUS),
      },
      {
        "previous phrase foo",
        new Selection.Builder(ObjectType.PHRASE)
          .setDirection(SearchDirection.PREVIOUS)
          .setName("foo"),
      },
      {
        "start of line",
        new Selection.Builder(ObjectType.LINE).setEndpoint(SelectionEndpoint.START),
      },
      {
        "start of word",
        new Selection.Builder(ObjectType.WORD).setEndpoint(SelectionEndpoint.START),
      },
      {
        "start of phrase foo",
        new Selection.Builder(ObjectType.PHRASE)
          .setEndpoint(SelectionEndpoint.START)
          .setName("foo"),
      },
      {
        "to next word",
        new Selection.Builder(ObjectType.WORD)
          .setDirection(SearchDirection.NEXT)
          .setFromCursorToObject(true),
      },
      {
        "to start of next word",
        new Selection.Builder(ObjectType.WORD)
          .setEndpoint(SelectionEndpoint.START)
          .setDirection(SearchDirection.NEXT)
          .setFromCursorToObject(true),
      },
      {
        "to end of next word",
        new Selection.Builder(ObjectType.WORD)
          .setEndpoint(SelectionEndpoint.END)
          .setDirection(SearchDirection.NEXT)
          .setFromCursorToObject(true),
      },
      {
        "to previous word",
        new Selection.Builder(ObjectType.WORD)
          .setDirection(SearchDirection.PREVIOUS)
          .setFromCursorToObject(true),
      },
      {
        "to start of previous word",
        new Selection.Builder(ObjectType.WORD)
          .setEndpoint(SelectionEndpoint.START)
          .setDirection(SearchDirection.PREVIOUS)
          .setFromCursorToObject(true),
      },
      {
        "to end of previous word",
        new Selection.Builder(ObjectType.WORD)
          .setEndpoint(SelectionEndpoint.END)
          .setDirection(SearchDirection.PREVIOUS)
          .setFromCursorToObject(true),
      },
      {
        "to last word",
        new Selection.Builder(ObjectType.WORD)
          .setAbsoluteRange(new Range(-1))
          .setFromCursorToObject(true),
      },
      {
        "to start of last word",
        new Selection.Builder(ObjectType.WORD)
          .setEndpoint(SelectionEndpoint.START)
          .setAbsoluteRange(new Range(-1))
          .setFromCursorToObject(true),
      },
      {
        "to end of last word",
        new Selection.Builder(ObjectType.WORD)
          .setEndpoint(SelectionEndpoint.END)
          .setAbsoluteRange(new Range(-1))
          .setFromCursorToObject(true),
      },
      {
        "to end of second to last word",
        new Selection.Builder(ObjectType.WORD)
          .setFromCursorToObject(true)
          .setEndpoint(SelectionEndpoint.END)
          .setAbsoluteRange(new Range(-2)),
      },
      {
        "last three words",
        new Selection.Builder(ObjectType.WORD).setAbsoluteRange(new Range(-3, 0)),
      },
    };

    for (Object[] arg : args) {
      assertEquals(((Selection.Builder) arg[1]).build(), convertSelection((String) arg[0]));
    }
  }
}
