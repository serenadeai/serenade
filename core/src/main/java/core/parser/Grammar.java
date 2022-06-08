package core.parser;

import com.google.common.collect.Lists;
import core.gen.antlr.antlr4.ANTLRv4Parser;
import core.gen.antlr.antlr4.ANTLRv4ParserBaseVisitor;
import core.parser.ParseTree;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import toolbelt.logging.Logs;

// This is an interface to the ANLTR command grammar we are using.

// This is similar to CommandGenerator in that it's using antlr to parse antlr.
// It will create `GrammarNode`s which correspond to possible tree structures that
// correspond to possible ParseTree outcomes.

// We should be able to ignore the Lexer portion of things since we already have a
// ParseTree (in MutableParseTree format) we're working with.

@Singleton
public class Grammar extends ANTLRv4ParserBaseVisitor<List<List<String>>> {

  // If we hit any of these number rules, we will remove all nested parser rules that we see.
  public static final Set<String> numberRules = new HashSet<String>(
    Arrays.asList("number", "numberRange1To10", "numberRange1To20", "numberRange1To99")
  );

  // These are special lexer rules that we ignore in grammar verification.
  private final List<String> ignoredLexerTokens = Arrays.asList(
    "DIGITS",
    "LITERAL_SYMBOL",
    "ALPHA",
    "ESCAPE_ALPHA",
    "THE_WORD_ALPHA"
  );

  private static Logger logger = LoggerFactory.getLogger(Grammar.class);

  public HashMap<String, GrammarNode> ruleToGrammarNode;
  private GrammarNode root;

  // This is the hardcoded number of times we will repeat +'s in the ANTLR grammar rules
  private int maxPlusRepeatTimes = 5;
  private int maxStarRepeatTimes = 5; // for *'s'

  @Inject
  GrammarAntlrParser grammarAntlrParser;

  @Inject
  public Grammar(GrammarAntlrParser grammarAntlrParser) {
    // Initialize the class with a parse of the grammar.
    this.ruleToGrammarNode = new HashMap<>();
    this.root = new GrammarNode("main");

    this.ruleToGrammarNode.put("main", this.root);

    try {
      org.antlr.v4.runtime.tree.ParseTree parserTree = grammarAntlrParser.getProductionParser();
      org.antlr.v4.runtime.tree.ParseTree lexerTree = grammarAntlrParser.getProductionLexer();
      visit(lexerTree);
      visit(parserTree);

      // We sometimes have grammar rules that are required but can be empty -- the `endpoint`
      // rule looks like `endpoint : (...)? ;`. Due to differences between the way sampling
      // and parsing works, we don't sample the `endpoint` rule at all when it's optional.
      // This makes for smaller parse trees, but also requires modifying the grammar to allow
      // optional nodes. We propagate up detected optional rules to augment the grammar.
      Map<String, Integer> orderedNodes = this.root.getChildDepthMap(0);
      List<String> processingOrder = orderedNodes
        .entrySet()
        .stream()
        .sorted(Comparator.comparingInt(x -> -x.getValue()))
        .map(x -> x.getKey())
        .collect(Collectors.toList());
      for (String name : processingOrder) {
        ruleToGrammarNode.get(name).propagateOptionalSequences();
      }
    } catch (IOException e) {
      Logs.logError(logger, "Error loading/reading grammar resources", e);
    }
  }

  public GrammarNode getNode(String type) {
    // Creates node if it does not exist.
    if (ruleToGrammarNode.get(type) == null) {
      GrammarNode newNode = new GrammarNode(type);
      ruleToGrammarNode.put(type, newNode);
      return newNode;
    } else {
      return ruleToGrammarNode.get(type);
    }
  }

  public boolean matchesGrammar(ParseTree tree) {
    return root.isParseTreeInGrammar(tree, false);
  }

  public String toString() {
    return this.root.collectAllowedChildSequences().toString();
  }

  public List<String> extractGrammarLexerLexicon() {
    Set<String> result = new HashSet<>();
    for (GrammarNode node : ruleToGrammarNode.values()) {
      if (node.isLexerNode) {
        result.addAll(
          node
            .extractMatchingLexerRules()
            .stream()
            .map(s -> s.replace("'", ""))
            .collect(Collectors.toSet())
        );
      }
    }
    return new ArrayList<>(result);
  }

  private List<List<String>> duplicateSequence(List<List<String>> sequence, int times) {
    List<List<String>> sequenceWithDups = new ArrayList<>();
    List<String> copyList;
    for (int i = 0; i < times; i++) {
      for (List<String> subsequence : sequence) {
        copyList = new ArrayList<>();
        for (int j = 0; j <= i; j++) {
          copyList.addAll(subsequence);
        }
        sequenceWithDups.add(copyList);
      }
    }
    return sequenceWithDups;
  }

  public List<List<String>> visitRuleref(ANTLRv4Parser.RulerefContext ctx) {
    String name = ctx.RULE_REF().getText();
    List<String> ruleSequence = Arrays.asList(name);
    return new ArrayList<>(Arrays.asList(ruleSequence));
  }

  public List<List<String>> visitBlock(ANTLRv4Parser.BlockContext ctx) {
    return visit(ctx.altList());
  }

  public List<List<String>> visitAtom(ANTLRv4Parser.AtomContext ctx) {
    if (ctx.DOT() != null) {
      return null;
    }
    return visit(ctx.getChild(0));
  }

  public List<List<String>> visitElement(ANTLRv4Parser.ElementContext ctx) {
    ANTLRv4Parser.EbnfSuffixContext suffix = ctx.ebnfSuffix();
    List<List<String>> sequence = null;
    if (ctx.labeledElement() != null) {
      if (ctx.labeledElement().atom() != null) {
        sequence = visit(ctx.labeledElement().atom());
      } else {
        sequence = visit(ctx.labeledElement().block());
      }
    } else if (ctx.atom() != null) {
      sequence = visit(ctx.atom());
    } else if (ctx.ebnf() != null) {
      sequence = visit(ctx.ebnf().block());
      if (ctx.ebnf().blockSuffix() != null) {
        suffix = ctx.ebnf().blockSuffix().ebnfSuffix();
      }
    }

    if (sequence == null) {
      return null;
    }
    if (suffix != null && suffix.QUESTION() != null && suffix.QUESTION().size() > 0) {
      sequence.add(Collections.emptyList());
      return sequence;
    } else if (suffix != null && suffix.STAR() != null) {
      sequence = duplicateSequence(sequence, maxStarRepeatTimes);
      sequence.add(Collections.emptyList());
      return sequence;
    } else if (suffix != null && suffix.PLUS() != null) {
      return duplicateSequence(sequence, maxPlusRepeatTimes);
    } else {
      return sequence;
    }
  }

  public List<List<String>> visitAlternative(ANTLRv4Parser.AlternativeContext ctx) {
    // Handle parser terms of the form: a b c
    List<List<List<String>>> typesByPosition = new ArrayList<>();
    for (ANTLRv4Parser.ElementContext child : ctx.element()) {
      List<List<String>> visitResults = visit(child);
      if (visitResults != null) {
        typesByPosition.add(visitResults);
      }
    }
    // After taking cartsian product of possibilities at each position, we need to flatten the
    // interior lists
    List<List<List<String>>> cartesianProductCommands = Lists.cartesianProduct(typesByPosition);
    List<List<String>> collapsedCommands = new ArrayList<>();
    for (List<List<String>> listToConcat : cartesianProductCommands) {
      collapsedCommands.add(
        listToConcat.stream().flatMap(List::stream).collect(Collectors.toList())
      );
    }
    return collapsedCommands;
  }

  public List<List<String>> visitAltList(ANTLRv4Parser.AltListContext ctx) {
    // Handle sub-rules of the form (a | b | c). See visitRuleAltList.
    List<List<String>> allSequences = new ArrayList<>();
    for (ANTLRv4Parser.AlternativeContext child : ctx.alternative()) {
      List<List<String>> visitResults = visit(child);
      allSequences.addAll(visitResults);
    }
    return allSequences;
  }

  public List<List<String>> visitRuleAltList(ANTLRv4Parser.RuleAltListContext ctx) {
    // Handles terms of the form ruleName: a | b | c.
    // Make sure the sub-rules are non-null.
    List<List<String>> allSequences = new ArrayList<>();
    for (ANTLRv4Parser.LabeledAltContext child : ctx.labeledAlt()) {
      List<List<String>> visitResults = visit(child.alternative());
      if (visitResults != null) {
        allSequences.addAll(visitResults);
      }
    }
    return allSequences;
  }

  @Override
  public List<List<String>> visitParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
    String name = ctx.RULE_REF().getText();
    GrammarNode currentNode = getNode(name);

    for (int i = 0; i < ctx.getChildCount(); i++) {
      List<List<String>> childSequences = visit(ctx.getChild(i));
      if (childSequences != null) {
        for (List<String> sequence : childSequences) {
          List<GrammarNode> nodeSequence = sequence
            .stream()
            .map(s -> getNode(s))
            .filter(node -> node.type.length() > 0)
            .collect(Collectors.toList());
          currentNode.addAllowedSequence(nodeSequence);
        }
      }
    }

    return new ArrayList<>(Arrays.asList(Arrays.asList(name)));
  }

  public List<List<String>> visitLexerElement(ANTLRv4Parser.LexerElementContext ctx) {
    ANTLRv4Parser.EbnfSuffixContext suffix = ctx.ebnfSuffix();
    List<List<String>> sequence = null;

    if (ctx.labeledLexerElement() != null) {
      if (ctx.labeledLexerElement().lexerAtom() != null) {
        sequence = visit(ctx.labeledLexerElement().lexerAtom());
      } else {
        sequence = visit(ctx.labeledLexerElement().lexerBlock());
      }
    } else if (ctx.lexerAtom() != null) {
      sequence = visit(ctx.lexerAtom());
    } else if (ctx.lexerBlock() != null) {
      sequence = visit(ctx.lexerBlock());
    }
    if (sequence == null) {
      return null;
    }
    if (suffix != null && suffix.QUESTION() != null && suffix.QUESTION().size() > 0) {
      sequence.add(Collections.emptyList());
      return sequence;
    } else if (suffix != null && suffix.STAR() != null) {
      sequence = duplicateSequence(sequence, maxStarRepeatTimes);
      sequence.add(Collections.emptyList());
      return sequence;
    } else if (suffix != null && suffix.PLUS() != null) {
      return duplicateSequence(sequence, maxPlusRepeatTimes);
    } else {
      return sequence;
    }
  }

  public List<List<String>> visitTerminal(ANTLRv4Parser.TerminalContext ctx) {
    if (ctx.STRING_LITERAL() != null) {
      String stringLiteral = ctx.STRING_LITERAL().getText();
      String ret = stringLiteral.substring(1, stringLiteral.length() - 1);
      return new ArrayList<>(Arrays.asList(Arrays.asList(stringLiteral)));
    } else if (ctx.TOKEN_REF() == null) {
      return null;
    }

    return new ArrayList<>(Arrays.asList(Arrays.asList(ctx.TOKEN_REF().getText())));
  }

  public List<List<String>> visitLexerBlock(ANTLRv4Parser.LexerBlockContext ctx) {
    return visit(ctx.lexerAltList());
  }

  public List<List<String>> visitLexerAltList(ANTLRv4Parser.LexerAltListContext ctx) {
    // Lexer equivalent of visitRuleAltList.
    List<List<String>> allSequences = new ArrayList<>();
    for (ANTLRv4Parser.LexerAltContext child : ctx.lexerAlt()) {
      List<List<String>> visitResults = visit(child.lexerElements());
      allSequences.addAll(visitResults);
    }
    return allSequences;
  }

  public List<List<String>> visitLexerElements(ANTLRv4Parser.LexerElementsContext ctx) {
    // Handle lexer terms of the form A B C.
    List<List<List<String>>> typesByPosition = new ArrayList<>();
    for (ANTLRv4Parser.LexerElementContext child : ctx.lexerElement()) {
      List<List<String>> visitResults = visit(child);
      if (visitResults != null) {
        typesByPosition.add(visitResults);
      }
    }
    // After taking cartsian product of possibilities at each position, we need to flatten the
    // interior lists
    List<List<List<String>>> cartesianProductCommands = Lists.cartesianProduct(typesByPosition);
    List<List<String>> collapsedCommands = new ArrayList<>();
    for (List<List<String>> listToConcat : cartesianProductCommands) {
      collapsedCommands.add(
        listToConcat.stream().flatMap(List::stream).collect(Collectors.toList())
      );
    }
    return collapsedCommands;
  }

  @Override
  public List<List<String>> visitLexerRuleSpec(ANTLRv4Parser.LexerRuleSpecContext ctx) {
    String name = ctx.TOKEN_REF().getText();

    if (ignoredLexerTokens.contains(name)) {
      // We need to manually treat particular tokens differently.
      return null;
    } else {
      GrammarNode currentNode = getNode(name);
      List<List<String>> childSequences = visit(ctx.lexerRuleBlock().lexerAltList());
      for (List<String> sequence : childSequences) {
        List<GrammarNode> nodeSequence = sequence
          .stream()
          .map(s -> getNode(s))
          .filter(node -> node.type.length() > 0)
          .collect(Collectors.toList());
        currentNode.addAllowedSequence(nodeSequence);
      }
      return new ArrayList<>(Arrays.asList(Arrays.asList(name)));
    }
  }
}
