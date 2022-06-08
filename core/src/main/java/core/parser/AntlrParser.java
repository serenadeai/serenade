package core.parser;

import core.ast.api.AstSyntaxError;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

public abstract class AntlrParser<ParserType extends Parser> {

  protected abstract Lexer lexer(CharStream input);

  protected abstract ParserType parser(CommonTokenStream tokenStream);

  protected abstract org.antlr.v4.runtime.tree.ParseTree rootAntlrParseTree(ParserType parser);

  private static class PanicModeErrorListener extends BaseErrorListener {

    // an exception that we can recover from using single token deletion or insertion.
    private Optional<RecognitionException> firstException = Optional.empty();

    public void syntaxError(
      Recognizer<?, ?> recognizer,
      Object offendingSymbol,
      int line,
      int charPositionInLine,
      String msg,
      RecognitionException e
    ) {
      if (e != null && firstException.isEmpty()) {
        firstException = Optional.of(e);
      }
    }
  }

  private Optional<ParseTree> convert(
    String source,
    CommonTokenStream tokens,
    org.antlr.v4.runtime.tree.ParseTree tree,
    Optional<ParseTree> parent
  ) {
    Interval interval = tree.getSourceInterval();
    int start = interval.a > -1 ? tokens.get(interval.a).getStartIndex() : -1;
    int stop = interval.b > -1 ? tokens.get(interval.b).getStopIndex() + 1 : -1;

    // whereas tree-sitter omits nodes, antlr creates empty nodes with a negative index.
    // so, don't include them in the tree, for consistency with tree-sitter.
    if (start == -1 || stop == -1) {
      return Optional.empty();
    }

    // in obfuscated code, classes look like `CommandParser$MainContext`, but we're always looking for
    // the name of only the inner-most class
    String name = tree instanceof TerminalNodeImpl
      ? source.substring(start, stop)
      : tree.getClass().getSimpleName();
    String[] split = name.split("\\$");

    // Sometimes the stop index is less than the start index, even after the off by one adjustment above.
    // This occurs when we have parser rules are allowed to be completely empty.
    // We use the max to prevent this from breaking the tree conversion, and there should be no corresponding
    // source segment in this case.
    ParseTree result = new ParseTree(
      split[split.length - 1],
      "",
      source,
      start,
      Math.max(start, stop),
      parent
    );
    List<ParseTree> children = new ArrayList<>();
    for (int i = 0; i < tree.getChildCount(); i++) {
      Optional<ParseTree> child = convert(source, tokens, tree.getChild(i), Optional.of(result));
      child.ifPresent(e -> children.add(e));
    }

    result.setChildren(children);
    return Optional.of(result);
  }

  private ParserType parserWithErrorHandling(
    CommonTokenStream tokenStream,
    PanicModeErrorListener panicModeErrorListener
  ) {
    ParserType parser = parser(tokenStream);
    parser.removeErrorListeners();
    parser.addErrorListener(panicModeErrorListener);
    return parser;
  }

  private AstSyntaxError syntaxError(RecognitionException exception) {
    Token token = exception.getOffendingToken();
    return new AstSyntaxError(
      token.getLine(),
      token.getCharPositionInLine(),
      token.getStartIndex()
    );
  }

  private CommonTokenStream tokens(String source) {
    CharStream input = CharStreams.fromString(source);
    Lexer lexer = lexer(input);
    lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
    CommonTokenStream result = new CommonTokenStream(lexer);
    result.fill();
    return result;
  }

  public ParseTree parse(String source) {
    PanicModeErrorListener panicModeErrorListener = new PanicModeErrorListener();
    CommonTokenStream tokens = tokens(source);
    ParserType parser = parserWithErrorHandling(tokens, panicModeErrorListener);
    org.antlr.v4.runtime.tree.ParseTree tree = rootAntlrParseTree(parser);
    panicModeErrorListener.firstException.ifPresent(
      e -> {
        throw syntaxError(e);
      }
    );
    return convert(source, tokens, tree, Optional.empty())
      .orElse(new ParseTree("", "", source, 0, 0, Optional.empty()));
  }

  public ParseTree convertAntlrTree(
    String source,
    CommonTokenStream tokens,
    org.antlr.v4.runtime.tree.ParseTree tree
  ) {
    return convert(source, tokens, tree, Optional.empty())
      .orElse(new ParseTree("", "", source, 0, 0, Optional.empty()));
  }
}
