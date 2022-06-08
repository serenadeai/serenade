package offline.subcommands;

import core.ast.Ast;
import core.ast.AstFactory;
import core.ast.api.AstList;
import core.ast.api.AstNode;
import core.ast.api.AstOptional;
import core.ast.api.AstParent;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import core.parser.Parser;
import corpusgen.mapping.CorpusGenAstFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import offline.Subcommand;
import toolbelt.languages.LanguageDeterminer;

@Singleton
public class TreePrinter implements Subcommand {

  @Inject
  LanguageDeterminer languageDeterminer;

  @Inject
  Parser parser;

  @Inject
  AstFactory astFactory;

  @Inject
  CorpusGenAstFactory corpusGenAstFactory;

  @Inject
  SmallRepositories smallRepositories;

  @Inject
  public TreePrinter() {}

  public void debug(
    Language language,
    List<String> nodeTypes,
    int limit,
    boolean parse,
    List<String> containersToClear,
    BiFunction<String, Language, AstParent> createRoot
  ) {
    List<String> sources = smallRepositories
      .paths(language)
      .stream()
      .map(
        path -> {
          try {
            return new String(Files.readAllBytes(path));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      )
      .collect(Collectors.toList());
    Collections.shuffle(sources);
    List<AstNode> nodes = sources
      .stream()
      .flatMap(
        source -> {
          try {
            return Stream.of(createRoot.apply(source, language));
          } catch (Exception e) {
            return Stream.empty();
          }
        }
      )
      .flatMap(root -> root.filter(e -> nodeTypes.contains(e.getClass().getSimpleName())))
      .limit(limit)
      .collect(Collectors.toList());
    for (AstNode node : nodes) {
      List<Ast.Comment> comments = node.tree().comments;
      while (comments.size() > 0) {
        comments.get(0).remove();
      }
      for (String containerToClear : containersToClear) {
        List<AstNode> containers = node
          .filter(e -> containerToClear.contains(e.getClass().getSimpleName()))
          .collect(Collectors.toList());
        for (AstNode container : containers) {
          if (container instanceof AstList) {
            ((AstList) container).clear();
          } else if (container instanceof AstOptional) {
            container = (AstOptional) container;
            ((AstOptional) container).clear();
          } else {
            container.remove();
          }
        }
      }
      System.out.println(node.code() + "\n");
      System.out.println(node.parseTree().get().toDebugString());
      if (parse) {
        System.out.println("\n" + node.toDebugString() + "\n");
      }
      System.out.println("===================================================================\n");
    }
  }

  public void print(String path, BiFunction<String, Language, AstParent> createRoot) {
    Language language = languageDeterminer.fromFilename(path);
    String source = "";
    try {
      source = new String(Files.readAllBytes(Paths.get(path)));
      ParseTree parseTree = parser.parse(source, language);
      System.out.println(parseTree.toDebugString());
    } catch (IOException e) {
      e.printStackTrace();
    }
    AstParent astTree = createRoot.apply(source, language);
    System.out.println(astTree.toDebugString());

    System.out.println("\n\nComments: \n");
    astTree
      .tree()
      .comments.stream()
      .forEach(comment -> System.out.println(comment.toDebugString()));
  }

  @Override
  public void configureSubparsers(Subparsers subparsers) {
    Subparser debug = subparsers
      .addParser("debug-trees")
      .help("Sample trees from source code files.");
    debug.addArgument("--language").type(String.class).help("Language used").required(true);
    debug
      .addArgument("--node-types")
      .type(String.class)
      .help("Classes of Ast nodes to sample, e.g. \"Function\"")
      .required(true);
    debug
      .addArgument("--limit")
      .type(String.class)
      .help("Number of files x nodes to sample (n = limit nodes total)");
    debug
      .addArgument("--parse")
      .type(Boolean.class)
      .action(Arguments.storeTrue())
      .help("Prints parse tree and AstTree if flag is set, just AstTree if not");
    debug
      .addArgument("--clear-containers")
      .type(String.class)
      .help("Classes of Ast nodes to clear");
    debug
      .addArgument("--corpusgen-parse")
      .type(Boolean.class)
      .action(Arguments.storeTrue())
      .help("Prints the corpusgen version of the tree");

    Subparser print = subparsers.addParser("print-tree").help("Print the tree for a given file.");
    print.addArgument("path").type(String.class).help("Source file to parse").required(true);
    print
      .addArgument("--corpusgen-parse")
      .type(Boolean.class)
      .action(Arguments.storeTrue())
      .help("Prints the corpusgen version of the tree");
  }

  @Override
  public void run(Namespace namespace) {
    String command = namespace.getString("command");

    BiFunction<String, Language, AstParent> createRoot = (s, l) -> astFactory.createFileRoot(s, l);
    if (namespace.getBoolean("corpusgen_parse")) {
      createRoot = (s, l) -> corpusGenAstFactory.createFileRoot(s, l);
    }

    if (command.equals("debug-trees")) {
      Language language = languageDeterminer.fromApiName(namespace.getString("language"));
      String nodeTypes = namespace.getString("node_types");
      List<String> nodeTypesList = Arrays.asList(nodeTypes.split(","));

      String limit = namespace.getString("limit");
      int limitInt = 10;
      if (limit != null) {
        limitInt = Integer.parseInt(limit);
      }

      String containersToClear = namespace.getString("clear-containers");
      List<String> containersToClearList = new ArrayList<>();
      if (containersToClear != null) {
        containersToClearList = Arrays.asList(containersToClear.split(","));
      }

      debug(
        language,
        nodeTypesList,
        limitInt,
        namespace.getBoolean("parse"),
        containersToClearList,
        createRoot
      );
    } else if (command.equals("print-tree")) {
      print(namespace.getString("path"), createRoot);
    }
  }
}
