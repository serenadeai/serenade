package corpusgen.command;

import core.parser.Grammar;
import core.parser.MutableParseTree;
import core.parser.MutableParseTreeFactory;
import core.parser.ParseTree;
import corpusgen.mapping.FullContextMapping;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class Chainer {

  Pattern letters = Pattern.compile("^[a-z]+$");

  @Inject
  CommandCanonicalizer commandCanonicalizer;

  @Inject
  Grammar grammar;

  @Inject
  MutableParseTreeFactory mutableParseTreeFactory;

  @Inject
  public Chainer() {}

  private MutableParseTree wrapNodesWithCommand(MutableParseTree node) {
    return wrapNodesWithCommand(Arrays.asList(node));
  }

  private MutableParseTree wrapNodesWithCommand(List<MutableParseTree> nodes) {
    MutableParseTree commandNode = new MutableParseTree("command");
    commandNode.setChildren(nodes);
    return commandNode;
  }

  private List<String> sampleCommandTokens(int count) {
    List<String> tokens = grammar.extractGrammarLexerLexicon();
    List<String> ret = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String sample;
      // Don't sample numbers.
      do {
        sample = tokens.get(ThreadLocalRandom.current().nextInt(0, tokens.size()));
      } while (!letters.matcher(sample).matches());
      ret.add(sample);
    }
    return ret;
  }

  private List<String> sampleReplaceCommandWords(List<String> sampledTokens) {
    List<String> modifiedTokens = new ArrayList<>();

    if (ThreadLocalRandom.current().nextDouble() < 0.02) {
      // Sample from our lexicon some of the time -- otherwise we can't use command words
      // in formatted text commands.
      int lengthModifiedTokens = 0;
      double percent = ThreadLocalRandom.current().nextDouble();
      if (percent < 0.7) {
        lengthModifiedTokens = 1;
      } else if (percent < 0.8) {
        lengthModifiedTokens = 2;
      } else if (percent < 0.9) {
        lengthModifiedTokens = 3;
      } else {
        lengthModifiedTokens = ThreadLocalRandom.current().nextInt(4, 6);
      }
      modifiedTokens = sampleCommandTokens(lengthModifiedTokens);
    } else if (ThreadLocalRandom.current().nextDouble() < 0.01) {
      // Mix our lexicon and the input text.
      for (int i = 0; i < sampledTokens.size(); i++) {
        if (ThreadLocalRandom.current().nextDouble() < 0.1) {
          modifiedTokens.addAll(sampleCommandTokens(1));
        } else {
          modifiedTokens.add(sampledTokens.get(i));
        }
      }
    } else {
      modifiedTokens = sampledTokens;
    }
    return modifiedTokens;
  }

  private Optional<List<String>> replaceTextPlaceholders(
    List<String> tokens,
    Supplier<Optional<List<String>>> formattedTextSampler
  ) {
    tokens = new ArrayList<>(tokens);
    while (tokens.contains("{{{formattedText}}}")) {
      Optional<List<String>> formattedTokens = formattedTextSampler.get();
      if (formattedTokens.isPresent()) {
        List<String> textTokens = sampleReplaceCommandWords(formattedTokens.get());
        int replaceIndex = tokens.indexOf("{{{formattedText}}}");
        tokens.remove(replaceIndex);
        tokens.addAll(replaceIndex, textTokens);
      } else {
        return Optional.empty();
      }
    }
    return Optional.of(tokens);
  }

  private MutableParseTree sampleNonText(CommandSamplers samplers) {
    List<String> commands = samplers.nonTextCommands;
    String command;
    if (ThreadLocalRandom.current().nextInt(3) == 0) {
      command = "goTo";
    } else {
      command = commands.get(ThreadLocalRandom.current().nextInt(commands.size()));
      if (command.equals("focus") && ThreadLocalRandom.current().nextInt(10) == 0) {
        while (command.equals("focus")) {
          command = commands.get(ThreadLocalRandom.current().nextInt(commands.size()));
        }
      }
    }
    return wrapNodesWithCommand(samplers.ruleToSampler.get(command).get());
  }

  protected Optional<ParseTree> generateChain(
    CommandSamplers samplers,
    Supplier<Optional<List<String>>> formattedTextSampler
  ) {
    List<MutableParseTree> chainNodes = new ArrayList<>();
    int remainingChainLength = 0;
    int textCommandCount = 0;
    boolean addPrepositionPostfix = false;
    boolean endWithRepeat = false; // This makes sure the last command in this chain is a
    // repeat command. This also works for non-chains.

    // Meta by themselves commands 20% of the time.
    double percent = ThreadLocalRandom.current().nextDouble();
    if (percent < 0.002) {
      chainNodes.addAll(samplers.ruleToSampler.get("unchainableCommand").get());
    } else if (percent < 0.2) {
      chainNodes.addAll(samplers.ruleToSampler.get("metaCommand").get());
    } else {
      double chainLengthPercent = ThreadLocalRandom.current().nextDouble();
      if (chainLengthPercent < 0.95) {
        remainingChainLength = 1;
      } else if (chainLengthPercent < 0.985) {
        remainingChainLength = 2;
      } else {
        remainingChainLength = 3;
      }
      textCommandCount = ThreadLocalRandom.current().nextInt(2);
      remainingChainLength -= textCommandCount;
      addPrepositionPostfix = ThreadLocalRandom.current().nextDouble() < 0.001;
      endWithRepeat =
        ThreadLocalRandom.current().nextDouble() < 0.001 &&
        !addPrepositionPostfix &&
        textCommandCount == 0;
    }

    // fill in non-text commands.
    while (chainNodes.size() < remainingChainLength) {
      if (remainingChainLength - chainNodes.size() == 1 && endWithRepeat) {
        chainNodes.add(samplers.ruleToSampler.get("repeat").get().get(0));
      } else {
        chainNodes.add(sampleNonText(samplers));
      }
    }

    // possibly end chain with text command.
    if (textCommandCount == 1) {
      String command = samplers.textCommands.get(
        ThreadLocalRandom.current().nextInt(samplers.textCommands.size())
      );
      chainNodes.add(wrapNodesWithCommand(samplers.ruleToSampler.get(command).get()));
      // Append a prepositional phrase if appropriate.
      if (addPrepositionPostfix) {
        chainNodes.addAll(samplers.ruleToSampler.get("prepositionSelection").get());
      } else if (chainNodes.size() == 1 && ThreadLocalRandom.current().nextDouble() < 0.2) {
        // Initial attempt at supporting chaining commands after text commands.
        if (chainNodes.get(0).children().get(0).type.equals("focus")) {
          chainNodes.add(sampleNonText(samplers));
        }
      }
    }
    List<String> chainMarkup = chainNodes
      .stream()
      .flatMap(node -> node.toParseTree().toMarkup().stream())
      .collect(Collectors.toList());
    return replaceTextPlaceholders(chainMarkup, formattedTextSampler)
      .filter(e -> e.size() < 100)
      .flatMap(e -> mutableParseTreeFactory.create(e))
      .map(
        e -> {
          commandCanonicalizer.canonicalize(e);
          return e.get(0).toParseTree();
        }
      );
  }

  public List<ParseTree> generateChains(
    CommandSamplers samplers,
    List<FullContextMapping> mappings
  ) {
    List<FullContextMapping> remainingMappings = new ArrayList<>(mappings);
    List<String> inputs = new ArrayList<>();
    List<String> outputs = new ArrayList<>();
    Supplier<Optional<List<String>>> formattedTextSampler = () -> {
      if (remainingMappings.size() > 0) {
        return Optional.of(
          Arrays.asList(
            remainingMappings.remove(remainingMappings.size() - 1).transcript().split(" ")
          )
        );
      }
      return Optional.empty();
    };

    List<ParseTree> result = new ArrayList<>();
    Optional<ParseTree> command = generateChain(samplers, formattedTextSampler);
    while (command.isPresent()) {
      result.add(command.get());
      command = generateChain(samplers, formattedTextSampler);
    }
    return result;
  }
}
