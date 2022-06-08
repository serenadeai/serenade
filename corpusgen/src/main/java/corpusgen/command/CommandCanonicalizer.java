package corpusgen.command;

import core.parser.Grammar;
import core.parser.MutableParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class CommandCanonicalizer {

  @Inject
  CommandCanonicalizer() {}

  private void replaceHelperRules(MutableParseTree tree) {
    List<MutableParseTree> newChildren = new ArrayList<>();
    for (MutableParseTree child : tree.children()) {
      if (Arrays.asList("addModifier", "addObject").contains(child.type)) {
        MutableParseTree newChild = new MutableParseTree("formattedText", child.text);
        newChild.setChildren(child.children());
        newChildren.add(newChild);
      } else {
        newChildren.add(child);
      }
    }
    tree.setChildren(newChildren);
    tree.children().stream().forEach(c -> replaceHelperRules(c));
  }

  private void mergeFormattedTextRules(MutableParseTree tree) {
    // Sometimes generated parsing can have nested or consecutive formatted text nodes. We should collapse these.
    tree.children().forEach(c -> mergeFormattedTextRules(c));

    // Merge nested formattedText nodes
    for (MutableParseTree child : tree.children()) {
      if (child.type.equals("formattedText")) {
        List<MutableParseTree> newChildren = new ArrayList<>();
        for (MutableParseTree nestedChild : child.children()) {
          if (nestedChild.type.equals("formattedText")) {
            newChildren.addAll(nestedChild.children());
          } else {
            newChildren.add(nestedChild);
          }
        }
        child.setChildren(newChildren);
      }
    }

    // Merge consecutive formattedText nodes
    List<MutableParseTree> childrenToDrop = new ArrayList<>();
    MutableParseTree previousChildIfFormattedText = null;
    for (MutableParseTree child : tree.children()) {
      if (child.type.equals("formattedText")) {
        if (previousChildIfFormattedText == null) {
          previousChildIfFormattedText = child;
        } else {
          previousChildIfFormattedText.children().addAll(child.children());
          childrenToDrop.add(child);
        }
      } else {
        previousChildIfFormattedText = null;
      }
    }
    childrenToDrop.stream().forEach(child -> tree.children().remove(child));
  }

  private List<MutableParseTree> extractFlatLexerNodes(MutableParseTree tree) {
    // Grammar number rules are nested -- this will remove the nesting if present.
    List<MutableParseTree> textNodes = new ArrayList<>();
    if (tree.children().size() > 0) {
      for (MutableParseTree child : tree.children()) {
        textNodes.addAll(extractFlatLexerNodes(child));
      }
    } else if (tree.type.equals("")) {
      return Arrays.asList(tree);
    } else {
      return Collections.emptyList();
    }
    return textNodes;
  }

  private void removeEmptyRules(MutableParseTree tree) {
    // Grammar rules can sometimes parse as rules with no children -- these nodes are unnecessary to keep.
    tree.children().stream().forEach(child -> removeEmptyRules(child));

    tree.setChildren(
      tree
        .children()
        .stream()
        .filter(
          child ->
            !(!child.type.equals("") && child.children().size() == 0 && child.text.equals(""))
        )
        .collect(Collectors.toList())
    );
  }

  private void flattenNumericRules(MutableParseTree tree) {
    if (Grammar.numberRules.contains(tree.type)) {
      tree.setChildren(extractFlatLexerNodes(tree));
    } else if (tree.children().size() > 0) {
      tree.children().stream().forEach(child -> flattenNumericRules(child));
    }
  }

  public void canonicalize(List<MutableParseTree> trees) {
    MutableParseTree main = new MutableParseTree("main");
    MutableParseTree commandChain = new MutableParseTree("commandChain");
    main.setChildren(Arrays.asList(commandChain));
    commandChain.setChildren(new ArrayList<>(trees));
    removeEmptyRules(main);
    flattenNumericRules(main);
    replaceHelperRules(main);
    mergeFormattedTextRules(main);
    trees.clear();
    trees.add(main);
  }
}
