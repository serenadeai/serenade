package core.parser;

import ai.serenade.treesitter.Languages;
import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.TreeCursor;
import ai.serenade.treesitter.TreeCursorNode;
import core.exception.CannotDetermineLanguage;
import core.gen.rpc.Language;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolbelt.env.Env;
import toolbelt.languages.LanguageDeterminer;
import toolbelt.logging.Logs;

@Singleton
public class Parser {

  @Inject
  LanguageDeterminer languageDeterminer;

  @Inject
  PostProcessor postProcessor;

  private Logger logger = LoggerFactory.getLogger(Parser.class);

  static {
    Env env = new Env();
    String path = env.sourceRoot() + "/core/src/dist/lib/libjava-tree-sitter";
    if (System.getenv("JAVA_TREE_SITTER") != null) {
      path = System.getenv("JAVA_TREE_SITTER");
    }

    path += System.getProperty("os.name").toLowerCase().contains("mac") ? ".dylib" : ".so";
    System.load(path);
  }

  @Inject
  public Parser() {}

  private ParseTree convertToParseTree(
    TreeCursor cursor,
    String source,
    Optional<ParseTree> parent
  ) {
    TreeCursorNode current = cursor.getCurrentTreeCursorNode();

    ParseTree result = new ParseTree(
      current.getType(),
      current.getName() != null ? current.getName() : "",
      source,
      current.getStartByte(),
      current.getEndByte(),
      parent
    );

    List<ParseTree> children = new ArrayList<>();
    if (cursor.gotoFirstChild()) {
      do {
        children.add(convertToParseTree(cursor, source, Optional.of(result)));
      } while (cursor.gotoNextSibling());

      cursor.gotoParent();
    }
    result.setChildren(children);

    // If this node is named, wrap the type node with the name node.
    if (current.getName() != null) {
      ParseTree nameTree = new ParseTree(
        current.getName(),
        current.getName(),
        source,
        current.getStartByte(),
        current.getEndByte(),
        parent
      );
      nameTree.setChildren(Arrays.asList(result));
      result.setParent(Optional.of(nameTree));
      return nameTree;
    }

    return result;
  }

  private long treeSitterLanguage(Language language) {
    if (language == Language.LANGUAGE_BASH) {
      return Languages.bash();
    } else if (language == Language.LANGUAGE_CSHARP) {
      return Languages.cSharp();
    } else if (language == Language.LANGUAGE_CPLUSPLUS) {
      return Languages.cpp();
    } else if (language == Language.LANGUAGE_DART) {
      return Languages.dart();
    } else if (language == Language.LANGUAGE_GO) {
      return Languages.go();
    } else if (language == Language.LANGUAGE_HTML) {
      return Languages.html();
    } else if (language == Language.LANGUAGE_JAVA) {
      return Languages.java();
    } else if (language == Language.LANGUAGE_JAVASCRIPT) {
      return Languages.tsx();
    } else if (language == Language.LANGUAGE_KOTLIN) {
      return Languages.kotlin();
    } else if (language == Language.LANGUAGE_PYTHON) {
      return Languages.python();
    } else if (language == Language.LANGUAGE_RUBY) {
      return Languages.ruby();
    } else if (language == Language.LANGUAGE_RUST) {
      return Languages.rust();
    } else if (language == Language.LANGUAGE_SCSS) {
      return Languages.scss();
    }

    throw new CannotDetermineLanguage();
  }

  public ParseTree parse(String source, Language language) throws UnsupportedEncodingException {
    return Logs.logTime(
      logger,
      "core.parse-source",
      Map.of("language", language),
      () -> {
        try {
          try (ai.serenade.treesitter.Parser parser = new ai.serenade.treesitter.Parser()) {
            parser.setLanguage(treeSitterLanguage(language));
            try (Tree tree = parser.parseString(source)) {
              try (TreeCursor cursor = tree.getRootNode().walk()) {
                ParseTree convertedTree = convertToParseTree(cursor, source, Optional.empty());

                postProcessor.postProcessParseTree(language, convertedTree);
                return convertedTree;
              }
            }
          }
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    );
  }
}
