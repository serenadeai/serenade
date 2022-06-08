package corpusgen.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import core.gen.rpc.Language;
import core.parser.Grammar;
import core.parser.GrammarAntlrParser;
import core.parser.ParseTree;
import corpusgen.CorpusGenComponent;
import corpusgen.DaggerCorpusGenComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class CommandGenerateTest {

  private CorpusGenComponent component = DaggerCorpusGenComponent.builder().build();

  private List<List<String>> formattedTextExamples = Arrays
    .asList(
      "system",
      "system dot out dot println",
      "command sampler",
      "number generator",
      "assert true"
    )
    .stream()
    .map(s -> Arrays.asList(s.split(" ")))
    .collect(Collectors.toList());

  private Supplier<Optional<List<String>>> formattedTextSampler = () -> {
    return Optional.of(
      formattedTextExamples.get(ThreadLocalRandom.current().nextInt(formattedTextExamples.size()))
    );
  };

  private void assertGeneratedCommandsInGrammar(int count) {
    try {
      Chainer chainer = component.chainer();
      Grammar grammar = component.grammar();
      GrammarAntlrParser grammarAntlrParser = component.grammarAntlrParser();
      CommandSamplers samplers = component.commandGenerator().generate(Language.LANGUAGE_JAVA);

      for (int i = 0; i < count; i++) {
        Optional<ParseTree> command = chainer.generateChain(samplers, formattedTextSampler);
        if (command.isPresent()) {
          assertTrue(
            grammar.matchesGrammar(command.get()),
            "Generated command not in grammar: " + command.get().toDebugString()
          );
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testGeneratedCommandsInGrammar() {
    assertGeneratedCommandsInGrammar(50000);
  }
}
