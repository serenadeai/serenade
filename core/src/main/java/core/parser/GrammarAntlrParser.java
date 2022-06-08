package core.parser;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import core.gen.antlr.antlr4.ANTLRv4Lexer;
import core.gen.antlr.antlr4.ANTLRv4Parser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.tree.ParseTree;

@Singleton
public class GrammarAntlrParser extends AntlrParser<ANTLRv4Parser> {

  @Inject
  public GrammarAntlrParser() {}

  protected Lexer lexer(CharStream input) {
    return new ANTLRv4Lexer(input);
  }

  protected ANTLRv4Parser parser(CommonTokenStream tokenStream) {
    return new ANTLRv4Parser(tokenStream);
  }

  protected ParseTree rootAntlrParseTree(ANTLRv4Parser parser) {
    return parser.grammarSpec();
  }

  private ParseTree parseGrammarFile(String resourceName) throws IOException {
    String source = Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
    Lexer lexer = this.lexer(CharStreams.fromString(source));
    ANTLRv4Parser parser = this.parser(new CommonTokenStream(lexer));
    return this.rootAntlrParseTree(parser);
  }

  public ParseTree getProductionLexer() throws IOException {
    return parseGrammarFile("CommandLexer.g4");
  }

  public ParseTree getProductionParser() throws IOException {
    return parseGrammarFile("CommandParser.g4");
  }
}
