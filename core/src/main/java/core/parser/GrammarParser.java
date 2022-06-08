package core.parser;

import core.gen.antlr.antlr4.ANTLRv4Lexer;
import core.gen.antlr.antlr4.ANTLRv4Parser;
import core.gen.antlr.antlr4.ANTLRv4ParserBaseVisitor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.inject.Inject;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

public class GrammarParser extends ANTLRv4ParserBaseVisitor<List<String>> {

  @Inject
  public GrammarParser() {}

  public ParseTree parse(String path) throws IOException {
    String source = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    CharStream input = CharStreams.fromString(source);
    ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
    ANTLRv4Parser parser = new ANTLRv4Parser(tokenStream);
    ParseTree tree = null;
    try {
      tree = parser.grammarSpec();
    } catch (ParseCancellationException e) {}
    return tree;
  }
}
