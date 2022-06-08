package core.parser;

import core.gen.antlr.command.CommandLexer;
import core.gen.antlr.command.CommandParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

@Singleton
public class CommandAntlrParser extends AntlrParser<CommandParser> {

  @Inject
  public CommandAntlrParser() {}

  protected Lexer lexer(CharStream input) {
    return new CommandLexer(input);
  }

  protected CommandParser parser(CommonTokenStream tokenStream) {
    return new CommandParser(tokenStream);
  }

  protected org.antlr.v4.runtime.tree.ParseTree rootAntlrParseTree(CommandParser parser) {
    return parser.main();
  }

  private ParseTree removeTypesOnLexerNodes(ParseTree tree) {
    List<ParseTree> newChildren = new ArrayList<>();
    for (ParseTree child : tree.getChildren()) {
      if (child.getChildren().size() == 0 && child.getCode().equals(child.getType())) {
        newChildren.add(
          new ParseTree(
            "",
            "",
            child.getSource(),
            child.getStart(),
            child.getStop(),
            child.getParent()
          )
        );
      } else {
        removeTypesOnLexerNodes(child);
        newChildren.add(child);
      }
    }
    tree.setChildren(newChildren);
    return tree;
  }

  private String normalizeNodeName(String name) {
    if (
      name.length() > 0 && Character.isUpperCase(name.toCharArray()[0]) && name.endsWith("Context")
    ) {
      return name.substring(0, 1).toLowerCase() + name.substring(1).replaceAll("Context$", "");
    }
    return name;
  }

  private ParseTree normalizeNodeNames(ParseTree tree) {
    List<ParseTree> newChildren = new ArrayList<>();
    for (ParseTree child : tree.getChildren()) {
      newChildren.add(normalizeNodeNames(child));
    }
    ParseTree newTree = new ParseTree(
      normalizeNodeName(tree.getType()),
      "",
      tree.getSource(),
      tree.getStart(),
      tree.getStop(),
      tree.getParent()
    );
    newTree.setChildren(newChildren);
    return newTree;
  }

  @Override
  public ParseTree convertAntlrTree(
    String source,
    CommonTokenStream tokens,
    org.antlr.v4.runtime.tree.ParseTree tree
  ) {
    return normalizeNodeNames(
      removeTypesOnLexerNodes(super.convertAntlrTree(source, tokens, tree))
    );
  }
}
