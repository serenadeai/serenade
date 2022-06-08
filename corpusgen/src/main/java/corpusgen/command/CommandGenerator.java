package corpusgen.command;

import core.gen.antlr.antlr4.ANTLRv4Parser;
import core.gen.antlr.antlr4.ANTLRv4ParserBaseVisitor;
import core.gen.rpc.Language;
import core.parser.Grammar;
import core.parser.GrammarNode;
import core.parser.GrammarParser;
import core.parser.MutableParseTree;
import core.parser.MutableParseTreeFactory;
import core.util.TextStyler;
import corpusgen.util.Apps;
import corpusgen.util.NumberGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.antlr.v4.runtime.tree.ParseTree;
import toolbelt.env.Env;

// This is an antlr visitor that visits an antlr grammar (yes, we are using
// antlr to parse antlr). It repeatedly does passes over the grammar, propogating
// the creation of "samplers", which are functions that randomly generate
// the parse trees of grammar rules. This is the place in code where we
// tweak the sampling of invidivual grammar rules or commands so that they're
// more representative of what people are doing (so this isn't the place where we
// tweak how often a command shows up, see Chainer for that).
//
// One thing this doesn't sample is actual text, which is replaced by a placeholder
// and replaced in Chainer.
//
// The main entry point is the generate function, which returns a CommandSamplers
// object.
public class CommandGenerator extends ANTLRv4ParserBaseVisitor<Supplier<List<MutableParseTree>>> {

  @Inject
  Apps apps;

  @Inject
  Env env;

  @Inject
  Grammar grammar;

  @Inject
  GrammarParser parser;

  @Inject
  MutableParseTreeFactory mutableParseTreeFactory;

  @Inject
  NumberGenerator numberGenerator;

  private double phraseSelectionProportion = 0.3;
  private Language language;
  private ParseTree lexerTree;
  private ParseTree parserTree;

  private HashMap<String, Supplier<List<MutableParseTree>>> ruleToSampler = new HashMap<>();
  private AlternativeNameVisitor nameVisitor = new AlternativeNameVisitor();
  private boolean ruleToSamplerChanged = true;
  private List<String> nonTextCommands = new ArrayList<>();
  private List<String> textCommands = new ArrayList<>();
  private List<String> quantifiableCommands = new ArrayList<>();

  @Inject
  public CommandGenerator() {}

  private List<MutableParseTree> toMutableParseTrees(String markup) {
    Optional<List<MutableParseTree>> trees = mutableParseTreeFactory.create(
      Arrays.asList(markup.trim().split("\\s+"))
    );
    if (!trees.isPresent()) {
      throw new RuntimeException(
        "Chainer sampling error -- markup not convertible to a tree: [" + markup + "]"
      );
    } else {
      return trees.get();
    }
  }

  public CommandSamplers generate(Language language) {
    this.language = language;
    try {
      this.lexerTree = parser.parse(env.sourceRoot() + "/core/src/main/resources/CommandLexer.g4");
      this.parserTree =
        parser.parse(env.sourceRoot() + "/core/src/main/resources/CommandParser.g4");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    updateTemplates(lexerTree);
    ruleToSamplerChanged = true;
    updateTemplates(parserTree);

    GrammarNode grammarRoot = grammar.getNode("main");
    GrammarNode commandsRoot = grammarRoot.getChild("commandChain").getChild("command");

    List<String> allCommands = commandsRoot
      .getAllowedChildren()
      .stream()
      .collect(Collectors.toList());
    for (String command : allCommands) {
      if (commandsRoot.isAllowedSequence(Arrays.asList(command, "quantifier"))) {
        quantifiableCommands.add(command);
        // These are mutually exclusive from text commands.
      } else if (commandsRoot.getChild(command).hasParserRuleDescendant("formattedText")) {
        textCommands.add(command);
      } else if (command.equals("quantifier")) {
        // Don't add this as a command type since we deal with it separately.
      } else {
        nonTextCommands.add(command);
      }
    }
    addQuantifierToQuantifiableCommandsTemplates();
    return new CommandSamplers(
      ruleToSampler,
      textCommands,
      Stream
        .concat(nonTextCommands.stream(), quantifiableCommands.stream())
        .collect(Collectors.toList())
    );
  }

  private void addSampler(
    String ruleName,
    Supplier<List<MutableParseTree>> sampler,
    boolean parser
  ) {
    if (!ruleToSampler.containsKey(ruleName) && sampler != null) {
      ruleToSamplerChanged = true;
      if (parser) {
        ruleToSampler.put(
          ruleName,
          () -> {
            MutableParseTree ret = new MutableParseTree(ruleName);
            ret.setChildren(sampler.get());
            return Arrays.asList(ret);
          }
        );
      } else {
        // don't put lexer rules in the tree
        ruleToSampler.put(ruleName, sampler);
      }
      return;
    }
  }

  private void addQuantifierToQuantifiableCommandsTemplates() {
    // Indent has its quantifiers in its templates, so just make this true with all of them.
    Supplier<List<MutableParseTree>> quantifierSampler = ruleToSampler.get("quantifier");
    for (String command : quantifiableCommands) {
      Supplier<List<MutableParseTree>> sampler = ruleToSampler.get(command);
      ruleToSampler.put(
        command,
        () -> {
          if (ThreadLocalRandom.current().nextDouble() < 0.9) {
            return sampler.get();
          }
          List<MutableParseTree> ret = new ArrayList<>();
          ret.addAll(sampler.get());
          ret.addAll(quantifierSampler.get());
          return ret;
        }
      );
    }
  }

  private void updateTemplates(ParseTree tree) {
    while (ruleToSamplerChanged) {
      ruleToSamplerChanged = false;
      visit(tree);
      List<String> ruleNames = new ArrayList<>(ruleToSampler.keySet());
      java.util.Collections.sort(ruleNames);
    }
  }

  private int sampleWeightedNumber() {
    double percent = ThreadLocalRandom.current().nextDouble();
    if (percent < 0.70) {
      return ThreadLocalRandom.current().nextInt(0, 10);
    } else if (percent < 0.75) {
      // Upsample tens
      return ThreadLocalRandom.current().nextInt(1, 10) * 10;
    } else if (percent < 0.93) {
      return ThreadLocalRandom.current().nextInt(1, 100);
    } else if (percent < 0.933) {
      // Special sampling for 100 and 1000
      return ThreadLocalRandom.current().nextDouble() < 0.8 ? 100 : 1000;
    } else if (percent < 0.98) {
      return ThreadLocalRandom.current().nextInt(1, 1000);
    } else {
      return ThreadLocalRandom.current().nextInt(1, 10000);
    }
  }

  public Supplier<List<MutableParseTree>> visitLexerRuleSpec(
    ANTLRv4Parser.LexerRuleSpecContext ctx
  ) {
    String name = ctx.TOKEN_REF().getText();
    Supplier<List<MutableParseTree>> sampler = null;
    // The lexer includes digits, so only generate the words.
    if (name.equals("ZERO")) {
      sampler = () -> toMutableParseTrees("zero");
    } else if (name.equals("ONE")) {
      sampler = () -> toMutableParseTrees("one");
    } else if (name.equals("TWO")) {
      sampler = () -> toMutableParseTrees("two");
    } else if (name.equals("THREE")) {
      sampler = () -> toMutableParseTrees("three");
    } else if (name.equals("FOUR")) {
      sampler = () -> toMutableParseTrees("four");
    } else if (name.equals("FIVE")) {
      sampler = () -> toMutableParseTrees("five");
    } else if (name.equals("SIX")) {
      sampler = () -> toMutableParseTrees("six");
    } else if (name.equals("SEVEN")) {
      sampler = () -> toMutableParseTrees("seven");
    } else if (name.equals("EIGHT")) {
      sampler = () -> toMutableParseTrees("eight");
    } else if (name.equals("NINE")) {
      sampler = () -> toMutableParseTrees("nine");
    } else if (!name.equals("LITERAL_SYMBOL")) {
      sampler = visit(ctx.lexerRuleBlock().lexerAltList());
    }
    addSampler(name, sampler, false);
    return null;
  }

  @Override
  public Supplier<List<MutableParseTree>> visitParserRuleSpec(
    ANTLRv4Parser.ParserRuleSpecContext ctx
  ) {
    String name = ctx.RULE_REF().getText();

    Supplier<List<MutableParseTree>> sampler = null;
    if (name.equals("formattedText")) {
      sampler = () -> toMutableParseTrees("{{{formattedText}}}");
    } else if (name.equals("phraseSelection") || name.equals("positionPhraseSelection")) {
      // Weighting for the base case (selecting a phrase with no modifiers) isn't high enough
      // so we want to bump that up a bit.
      Supplier<List<MutableParseTree>> phraseSampler = visit(ctx.ruleBlock().ruleAltList());
      if (phraseSampler != null) {
        sampler =
          () ->
            ThreadLocalRandom.current().nextInt(10) < 5
              ? phraseSampler.get()
              : toMutableParseTrees("<formattedText> {{{formattedText}}} </formattedText>");
      }
    } else if (name.equals("unnamedSelection") || name.equals("unnamedPositionSelection")) {
      Supplier<List<MutableParseTree>> phraseSampler = visit(ctx.ruleBlock().ruleAltList());
      if (phraseSampler != null) {
        // We want to upsample object selection relative to phrase selections -- there's lots of selectable objects but
        // only `one` phrase selection.
        sampler =
          () ->
            ThreadLocalRandom.current().nextInt(10) < 9
              ? ruleToSampler.get("selectionObjectSingular").get()
              : phraseSampler.get();
      }
    } else if (name.equals("namedSelection") || name.equals("namedPositionSelection")) {
      Supplier<List<MutableParseTree>> phraseSampler = visit(ctx.ruleBlock().ruleAltList());
      if (phraseSampler != null) {
        sampler =
          () -> {
            if (ThreadLocalRandom.current().nextInt(10) < 9) {
              return phraseSampler.get();
            }
            List<MutableParseTree> ret = new ArrayList<>();
            ret.addAll(ruleToSampler.get("namedSelectionObject").get());
            ret.addAll(toMutableParseTrees("<formattedText> {{{formattedText}}} </formattedText>"));
            return ret;
          };
      }
    } else if (name.equals("number")) {
      sampler =
        () -> {
          return toMutableParseTrees(numberGenerator.sampleEnglish(sampleWeightedNumber()));
        };
    } else if (name.equals("numberRange1To99")) {
      sampler =
        () -> {
          double percent = ThreadLocalRandom.current().nextDouble();
          if (percent < 0.80) {
            return toMutableParseTrees(
              numberGenerator.sampleEnglish(ThreadLocalRandom.current().nextInt(1, 10))
            );
          } else {
            return toMutableParseTrees(
              numberGenerator.sampleEnglish(ThreadLocalRandom.current().nextInt(1, 100))
            );
          }
        };
    } else if (name.equals("numberRange1To10")) {
      sampler =
        () -> {
          return toMutableParseTrees(
            numberGenerator.sampleEnglish(ThreadLocalRandom.current().nextInt(1, 10))
          );
        };
    } else if (name.equals("range")) {
      sampler =
        () -> {
          int rangeStart = sampleWeightedNumber();
          int rangeEnd = rangeStart + sampleWeightedNumber();

          List<MutableParseTree> result = new ArrayList<>();
          result.addAll(
            toMutableParseTrees(
              "<number> " + numberGenerator.sampleEnglish(rangeStart) + " </number>"
            )
          );
          result.addAll(ruleToSampler.get("rangeAdverb").get());
          result.addAll(
            toMutableParseTrees("<number>" + numberGenerator.sampleEnglish(rangeEnd) + "</number>")
          );
          return result;
        };
    } else if (name.equals("press")) {
      sampler =
        () -> {
          if (ThreadLocalRandom.current().nextBoolean()) {
            return ruleToSampler.get("implicitKey").get();
          }
          List<MutableParseTree> ret = new ArrayList<>(toMutableParseTrees("press"));
          int modifierLength = ThreadLocalRandom.current().nextInt(4);
          for (int i = 0; i < modifierLength; i++) {
            ret.addAll(ruleToSampler.get("modifierKey").get());
          }
          if (modifierLength == 0 || ThreadLocalRandom.current().nextDouble() < 0.8) {
            ret.addAll(ruleToSampler.get("key").get());
            while (ThreadLocalRandom.current().nextInt(10) == 0) {
              ret.addAll(ruleToSampler.get("key").get());
            }
          }
          return ret;
        };
    } else if (name.equals("focus")) {
      sampler =
        () ->
          toMutableParseTrees(
            "focus <formattedText> " +
            (
              ThreadLocalRandom.current().nextInt(4) < 3
                ? apps.common.get(ThreadLocalRandom.current().nextInt(apps.common.size()))
                : "{{{formattedText}}}"
            ) +
            " </formattedText>"
          );
    } else if (name.equals("add")) {
      sampler = null;
    } else if (name.equals("addOld")) {
      name = "add";
      // upsample "add x" to 50% of add commands.
      Supplier<List<MutableParseTree>> addSampler = visit(ctx.ruleBlock().ruleAltList());
      if (addSampler != null) {
        sampler =
          () ->
            ThreadLocalRandom.current().nextBoolean()
              ? toMutableParseTrees(
                "<addPrefix> add </addPrefix> <formattedText> {{{formattedText}}} </formattedText>"
              )
              : addSampler.get();
      }
    } else if (name.equals("addObject")) {
      Supplier<List<MutableParseTree>> optionSampler = visit(ctx.ruleBlock().ruleAltList());
      if (optionSampler != null) {
        sampler =
          () -> {
            // Wrap addObject in formattedText since it's a hack we have in place to improve accuracy on snippets,
            // but we don't actually want the parse tree. The tree gets flattened in Chainer->CanonicalMarkup.
            MutableParseTree formattedTextNode = new MutableParseTree("formattedText", "");
            formattedTextNode.setChildren(optionSampler.get());
            return Arrays.asList(formattedTextNode);
          };
      }
    } else if (name.equals("addModifier")) {
      Supplier<List<MutableParseTree>> optionSampler = visit(ctx.ruleBlock().ruleAltList());
      if (optionSampler != null) {
        sampler =
          () -> {
            List<MutableParseTree> ret = new ArrayList<>();
            int numModifiers = ThreadLocalRandom.current().nextInt(2);
            for (int i = 0; i < numModifiers; i++) {
              String modifierType = optionSampler.get().get(0).text;
              ret.addAll(toMutableParseTrees(modifierType));
            }
            return ret;
          };
      }
    } else if (name.equals("selectionWithImplicitPhrase")) {
      sampler =
        () ->
          ThreadLocalRandom.current().nextDouble() < phraseSelectionProportion
            ? ruleToSampler.get("phraseSelection").get()
            : ruleToSampler.get("selection").get();
    } else if (name.equals("selection")) {
      sampler =
        () ->
          ThreadLocalRandom.current().nextDouble() < phraseSelectionProportion
            ? ruleToSampler.get("namedSelection").get()
            : ruleToSampler.get("unnamedSelection").get();
    } else if (name.equals("positionSelection")) {
      sampler =
        () -> {
          double percent = ThreadLocalRandom.current().nextDouble();
          if (percent < phraseSelectionProportion) {
            return ruleToSampler.get("namedPositionSelection").get();
          } else {
            return ruleToSampler.get("unnamedPositionSelection").get();
          }
        };
    } else if (name.equals("selectionObjectSingular")) {
      Supplier<List<MutableParseTree>> optionSampler = visit(ctx.ruleBlock().ruleAltList());
      if (optionSampler != null) {
        sampler =
          () -> {
            int randInt = ThreadLocalRandom.current().nextInt(20);
            if (randInt == 0) {
              return toMutableParseTrees("<lineSingularObject> line </lineSingularObject>");
            } else if (randInt == 1) {
              return toMutableParseTrees(
                "<parameterSingularObject> parameter </parameterSingularObject>"
              );
            } else {
              return optionSampler.get();
            }
          };
      }
    } else if (name.equals("quantifier")) {
      sampler =
        () -> {
          double percent = ThreadLocalRandom.current().nextDouble();
          if (percent < 0.01) {
            return toMutableParseTrees("once");
          } else if (percent < 0.06) {
            return toMutableParseTrees("twice");
          } else if (percent < 0.07) {
            return toMutableParseTrees("thrice");
          } else {
            List<MutableParseTree> result = new ArrayList<>(
              ruleToSampler.get("numberRange1To99").get()
            );
            String number = result
              .stream()
              .flatMap(tree -> tree.toParseTree().toMarkup().stream())
              .collect(Collectors.joining(" "));
            String one = "<numberRange1To99> one </numberRange1To99>";
            boolean useTime = number.equals(one) == ThreadLocalRandom.current().nextDouble() < 0.99;
            result.addAll(toMutableParseTrees(useTime ? "time" : "times"));
            return result;
          }
        };
    } else {
      sampler = visit(ctx.ruleBlock().ruleAltList());
    }

    addSampler(name, sampler, true);
    return null;
  }

  public Supplier<List<MutableParseTree>> visitAlternative(ANTLRv4Parser.AlternativeContext ctx) {
    // Handle parser terms of the form: a b c
    List<Supplier<List<MutableParseTree>>> partialSamplers = new ArrayList<>();
    for (ANTLRv4Parser.ElementContext child : ctx.element()) {
      Supplier<List<MutableParseTree>> sampler = visit(child);
      if (sampler == null) {
        return null;
      }
      partialSamplers.add(sampler);
    }
    // Concatenate the output of the elements.
    return () ->
      partialSamplers.stream().flatMap(s -> s.get().stream()).collect(Collectors.toList());
  }

  public Supplier<List<MutableParseTree>> visitBlock(ANTLRv4Parser.BlockContext ctx) {
    return visit(ctx.altList());
  }

  public Supplier<List<MutableParseTree>> visitLexerBlock(ANTLRv4Parser.LexerBlockContext ctx) {
    return visit(ctx.lexerAltList());
  }

  public Supplier<List<MutableParseTree>> visitAtom(ANTLRv4Parser.AtomContext ctx) {
    if (ctx.DOT() != null) {
      return null;
    }
    return visit(ctx.getChild(0));
  }

  public Supplier<List<MutableParseTree>> visitTerminal(ANTLRv4Parser.TerminalContext ctx) {
    if (ctx.STRING_LITERAL() != null) {
      String stringLiteral = ctx.STRING_LITERAL().getText();
      String ret = stringLiteral.substring(1, stringLiteral.length() - 1);
      return () -> toMutableParseTrees(ret);
    } else if (ctx.TOKEN_REF() == null) {
      return null;
    }
    return ruleToSampler.get(ctx.TOKEN_REF().getText());
  }

  public Supplier<List<MutableParseTree>> visitRuleref(ANTLRv4Parser.RulerefContext ctx) {
    return ruleToSampler.get(ctx.RULE_REF().getText());
  }

  public Supplier<List<MutableParseTree>> visitAltList(ANTLRv4Parser.AltListContext ctx) {
    // Handle sub-rules of the form (a | b | c). See visitRuleAltList.
    List<Supplier<List<MutableParseTree>>> ret = new ArrayList<>();
    for (ANTLRv4Parser.AlternativeContext child : ctx.alternative()) {
      Supplier<List<MutableParseTree>> sampler = visit(child);
      if (sampler == null) {
        return null;
      }
      ret.add(sampler);
    }
    return () -> ret.get(ThreadLocalRandom.current().nextInt(ret.size())).get();
  }

  public Supplier<List<MutableParseTree>> visitElement(ANTLRv4Parser.ElementContext ctx) {
    ANTLRv4Parser.EbnfSuffixContext suffix = ctx.ebnfSuffix();
    Supplier<List<MutableParseTree>> sampler;
    if (ctx.labeledElement() != null) {
      if (ctx.labeledElement().atom() != null) {
        sampler = visit(ctx.labeledElement().atom());
      } else {
        sampler = visit(ctx.labeledElement().block());
      }
    } else if (ctx.atom() != null) {
      sampler = visit(ctx.atom());
    } else if (ctx.ebnf() != null) {
      sampler = visit(ctx.ebnf().block());
      if (ctx.ebnf().blockSuffix() != null) {
        suffix = ctx.ebnf().blockSuffix().ebnfSuffix();
      }
    } else if (ctx.actionBlock() != null) { // Ignore the predicates for checking if java or python.
      return () -> Collections.emptyList();
    } else {
      sampler = null;
    }

    if (sampler == null) {
      return null;
    }
    // Sample optionals with 50% chance. Appears to also include +, but I haven't reevaluated
    // this in a while. Sequences are usually special cased (they're either chaining or text).
    if (
      suffix != null &&
      (suffix.QUESTION() != null || suffix.PLUS() != null || suffix.STAR() != null)
    ) {
      return () ->
        ThreadLocalRandom.current().nextBoolean() ? Collections.emptyList() : sampler.get();
    }
    return sampler;
  }

  public Supplier<List<MutableParseTree>> visitLexerElement(ANTLRv4Parser.LexerElementContext ctx) {
    ANTLRv4Parser.EbnfSuffixContext suffix = ctx.ebnfSuffix();
    Supplier<List<MutableParseTree>> sampler;
    if (ctx.labeledLexerElement() != null) {
      if (ctx.labeledLexerElement().lexerAtom() != null) {
        sampler = visit(ctx.labeledLexerElement().lexerAtom());
      } else {
        sampler = visit(ctx.labeledLexerElement().lexerBlock());
      }
    } else if (ctx.lexerAtom() != null) {
      sampler = visit(ctx.lexerAtom());
    } else if (ctx.lexerBlock() != null) {
      sampler = visit(ctx.lexerBlock());
    } else {
      return null;
    }
    if (sampler == null) {
      return null;
    }
    if (
      suffix != null &&
      (suffix.QUESTION() != null || suffix.PLUS() != null || suffix.STAR() != null)
    ) {
      return () ->
        ThreadLocalRandom.current().nextBoolean() ? Collections.emptyList() : sampler.get();
    }
    return sampler;
  }

  public Supplier<List<MutableParseTree>> visitRuleAltList(ANTLRv4Parser.RuleAltListContext ctx) {
    // Handles terms of the form ruleName: a | b | c.
    // Make sure the sub-rules are non-null.
    List<Supplier<List<MutableParseTree>>> ret = new ArrayList<>();
    for (ANTLRv4Parser.LabeledAltContext child : ctx.labeledAlt()) {
      Supplier<List<MutableParseTree>> sampler = visit(child.alternative());
      if (sampler == null) {
        return null;
      }
      ret.add(sampler);
    }
    // Pick among the alternatives uniformly.
    return () -> ret.get(ThreadLocalRandom.current().nextInt(ret.size())).get();
  }

  public Supplier<List<MutableParseTree>> visitLexerAltList(ANTLRv4Parser.LexerAltListContext ctx) {
    // Lexer equivalent of visitRuleAltList.
    List<Supplier<List<MutableParseTree>>> ret = new ArrayList<>();
    for (ANTLRv4Parser.LexerAltContext child : ctx.lexerAlt()) {
      Supplier<List<MutableParseTree>> sampler = visit(child.lexerElements());
      if (sampler == null) {
        return null;
      }
      ret.add(sampler);
    }
    return () -> ret.get(ThreadLocalRandom.current().nextInt(ret.size())).get();
  }

  public Supplier<List<MutableParseTree>> visitLexerElements(
    ANTLRv4Parser.LexerElementsContext ctx
  ) {
    // Handle lexer terms of the form A B C.
    List<Supplier<List<MutableParseTree>>> partialSamplers = new ArrayList<>();
    for (ANTLRv4Parser.LexerElementContext child : ctx.lexerElement()) {
      Supplier<List<MutableParseTree>> sampler = visit(child);
      if (sampler == null) {
        return null;
      }
      partialSamplers.add(sampler);
    }
    // concatenate the sub-rules.
    return () ->
      partialSamplers.stream().flatMap(s -> s.get().stream()).collect(Collectors.toList());
  }
}
