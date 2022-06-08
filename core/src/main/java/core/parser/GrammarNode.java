package core.parser;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import core.util.NumberConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

// Used to build a tree structure that represents allowable rule patterns for the types in our grammar.
// This is a building block to allow checking validity of a markup string since we can trace the
// tree in the markup string against the allowable structure.

public class GrammarNode {

  private NumberConverter numberConverter;
  public final String type;
  public final boolean isLexerNode;

  private final String formattedTextNode = "formattedText";

  // These contain children for both Parser nodes and Lexer nodes. Parser nodes are
  // camelCase and Lexer nodes are all UPPERCASE, or are 'singlequotedlowercase'.
  // Lexer children should always correspond to a MutableParseTree with type ""
  // while Parser children should have their type match itself.
  private Map<String, GrammarNode> allowedChildren = new HashMap<>();

  // Not all combinations of children are allowed in any sequence.
  // We exhaust all the possiblities, accounting for optional or repeatable tags.
  private Set<List<String>> allowedChildSequences = new HashSet<>();

  // Maps a string to a valid lexer tag. The string has single-quotes stripped.
  // We apply this map to incoming nodes/markup so we can match with the allowed
  // child sequences more easily.
  private Map<String, String> stringToLexerRule = new HashMap<>();

  public GrammarNode(String type) {
    this.type = type;
    this.isLexerNode = type.toUpperCase().equals(type) || type.startsWith("'");
    this.numberConverter = new NumberConverter();
  }

  public List<String> extractMatchingLexerRules() {
    // Grab all child lexer rules from this node.
    if (!isLexerNode) {
      return Arrays.asList();
    } else if (type.startsWith("'")) {
      return Arrays.asList(type);
    } else {
      List<String> ret = new ArrayList<>();
      for (GrammarNode child : allowedChildren.values()) {
        if (child.type.startsWith("'")) {
          ret.add(child.type);
        } else {
          ret.addAll(child.extractMatchingLexerRules());
        }
      }
      return ret;
    }
  }

  private void addAllowedChild(GrammarNode node) {
    if (!allowedChildren.containsKey(node.type)) {
      allowedChildren.put(node.type, node);

      if (node.isLexerNode) {
        // Need to add lexer rules to the map on node additions.
        List<String> childMatches = node.extractMatchingLexerRules();
        for (String s : childMatches) {
          stringToLexerRule.put(s.replace("'", ""), node.type);
        }
      }
    }
  }

  public void addAllowedSequence(List<GrammarNode> nodes) {
    if (nodes.size() > 0) {
      for (GrammarNode node : nodes) {
        addAllowedChild(node);
      }
    }
    addAllowedSubsequence(nodes);
  }

  private void addAllowedSubsequence(List<GrammarNode> nodes) {
    // Version of the above that does not need to add new children
    if (nodes.size() > 0) {
      List<String> typeSequence = nodes
        .stream()
        .map(n -> n.type)
        .filter(s -> !(s.equals("EOF")))
        .collect(Collectors.toList());
      allowedChildSequences.add(typeSequence);
    } else {
      // Allow completely optional rules.
      allowedChildSequences.add(Collections.emptyList());
    }
  }

  protected Map<String, Integer> getChildDepthMap(int currentDepth) {
    // Returns a list of all child nodes with their max depth.

    Map<String, Integer> result = new HashMap<>();
    for (GrammarNode child : allowedChildren.values()) {
      if (!child.isLexerNode) {
        Map<String, Integer> childMap = child.getChildDepthMap(currentDepth + 1);
        for (String name : childMap.keySet()) {
          result.put(name, Math.max(result.getOrDefault(name, 0), childMap.get(name)));
        }
      }
    }

    if (!result.containsKey(this.type)) {
      result.put(this.type, currentDepth);
    }
    return result;
  }

  protected void propagateOptionalSequences() {
    // If a child is optional ([] is an allowed sequence for the child), we propagate that
    // optionality to the caller node.
    Set<String> optionalChildren = allowedChildren
      .values()
      .stream()
      .filter(
        child ->
          !child.type.equals(formattedTextNode) && child.isAllowedSequence(Collections.emptyList())
      )
      .map(child -> child.type)
      .collect(Collectors.toSet());

    if (optionalChildren.size() > 0) {
      Set<List<String>> originalChildSequences = new HashSet<>(allowedChildSequences);
      // For each existing allowed sequence, we optionally remove the optional components
      // and add the resulting sequence as allowed.
      // We do it this way to handle multiple optional components.
      for (List<String> allowedSeq : originalChildSequences) {
        List<List<String>> allowedSeqWithOptionals = allowedSeq
          .stream()
          .map(s -> optionalChildren.contains(s) ? Arrays.asList(s, "") : Arrays.asList(s))
          .collect(Collectors.toList());
        List<List<String>> cartesianProductSequences = Lists.cartesianProduct(
          allowedSeqWithOptionals
        );

        for (List<String> newSequence : cartesianProductSequences) {
          List<GrammarNode> newGrammarNodeSequence = newSequence
            .stream()
            .filter(s -> s.length() > 0)
            .map(s -> allowedChildren.get(s))
            .collect(Collectors.toList());
          addAllowedSubsequence(newGrammarNodeSequence);
        }
      }
    }
  }

  public Set<String> getAllowedChildren() {
    return allowedChildren.keySet();
  }

  public Set<List<String>> getAllowedChildSequences() {
    return allowedChildSequences;
  }

  public GrammarNode getChild(String type) {
    return allowedChildren.get(type);
  }

  public boolean isAllowedSequence(List<String> input) {
    // Generally use isParseTreeInGrammar since that works recursively. This is used for determining quantifier commands.
    return allowedChildSequences.contains(input);
  }

  protected boolean isParseTreeInGrammar(ParseTree node) {
    return isParseTreeInGrammar(node, true);
  }

  private boolean isParseTreeLexerNode(ParseTree node) {
    return node.isTerminal() && !node.getCode().equals("");
  }

  protected boolean isParseTreeInGrammar(ParseTree node, boolean verbose) {
    // This checks whether the given ParseTree and its children are valid in this grammar.

    String nodeType = isParseTreeLexerNode(node) ? node.getCode().toUpperCase() : node.getType();
    if (verbose) {
      System.out.println("Checking grammar node: " + type);
    }

    // Checking current node:
    if (!type.equals(nodeType)) {
      if (verbose) {
        System.out.println(
          "Failed match -- Current GrammarNode: " + type + " :: Input node: " + nodeType
        );
      }
      return false;
    }

    // If node is a formattedText node, all children need to be tokens.
    if (type.equals(formattedTextNode)) {
      return node.getChildren().stream().allMatch(child -> isParseTreeLexerNode(child));
    } else if (Grammar.numberRules.contains(nodeType)) {
      return numberConverter.isValid(node.getCode());
    }

    // Need to tokenize child nodes. Tokens should be converted to lexer matches (upper case).
    List<String> inputChildSequence = node
      .getChildren()
      .stream()
      .map(
        child -> {
          if (isParseTreeLexerNode(child)) {
            return child.getCode().toUpperCase();
          } else {
            return child.getType();
          }
        }
      )
      .collect(Collectors.toList());
    if (!isAllowedSequence(inputChildSequence)) {
      if (verbose) {
        System.out.println(
          "Failed match -- Current GrammarNode: " +
          type +
          " :: Input sequence: " +
          inputChildSequence +
          "\n Input node: " +
          node
        );
        System.out.println("Acceptable matches: " + collectAllowedChildSequences());
      }
      return false;
    }
    // Recurse on children for non-token and non-keyword nodes.
    if (node.getChildren().size() == 0) {
      return true;
    }
    boolean allChildrenInGrammar = node
      .getChildren()
      .stream()
      .filter(c -> !(isParseTreeLexerNode(c)))
      .allMatch(c -> getChild(c.getType()).isParseTreeInGrammar(c, verbose));
    return allChildrenInGrammar;
  }

  public boolean hasParserRuleDescendant(String rule) {
    // Checks if this Parser rule type ever appears as some descendant of this node.
    // This does not check if the current node's type matches `rule`.

    if (allowedChildren.size() == 0) {
      return false;
    }
    return (
      allowedChildren.containsKey(rule) ||
      allowedChildren.values().stream().anyMatch(child -> child.hasParserRuleDescendant(rule))
    );
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("type", type)
      .add("isLexerNode", isLexerNode)
      .add("allowedChildren", allowedChildren)
      .add("allowedChildSequences", allowedChildSequences)
      .add("stringToLexerRule", stringToLexerRule)
      .toString();
  }

  public Map<String, String> collectAllowedChildSequences() {
    // This is a toString helper at the Grammar level.
    // Extracts allowed subsequences for all GrammarNodes, instead of the entire GrammarNode info.
    // Intended for checking that we are representing the ANTLR grammar properly.
    // Stores results in a map so we can combine them recursively.
    Map<String, String> results = new HashMap<>();

    String layerResult;
    if (allowedChildren.size() == 0) {
      layerResult = " [ NODE Type: " + type + " , isLexer: " + Boolean.toString(isLexerNode) + "] ";
    } else {
      String childrenSequences = "";
      for (List<String> seq : allowedChildSequences) {
        childrenSequences += seq.stream().collect(Collectors.joining(", ")) + "\n";
      }
      layerResult =
        " [ NODE Type: " +
        type +
        " , isLexer: " +
        Boolean.toString(isLexerNode) +
        " \n " +
        childrenSequences +
        " \n " +
        " ] ";
    }
    results.put(type, layerResult);
    if (allowedChildren.size() > 0) {
      for (String type : allowedChildren.keySet()) {
        results.putAll(allowedChildren.get(type).collectAllowedChildSequences());
      }
    }
    return results;
  }
}
