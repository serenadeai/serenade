package core.codeengine;

import core.ast.api.AstContainer;
import core.ast.api.AstParent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class InputConverter {

  private static int maxSubsequencesSize = 50;
  private static int maxContextForAlphaSubsequences = 1500;

  @Inject
  AlphaNumericFinder alphaNumericFinder;

  @Inject
  public InputConverter() {}

  private List<List<Tokenizer.Token>> alphaSubsequencesContext(
    Map<List<Tokenizer.Token>, List<String>> alphaNumerics,
    List<String> words,
    double keepProportion
  ) {
    // Probably not an issue, but shuffling to avoid any positional biases that might
    // differ between offline / online evaluation.
    List<List<Tokenizer.Token>> shuffledSubsequences = alphaNumerics
      .entrySet()
      .stream()
      .filter(e -> Collections.indexOfSubList(words, e.getValue()) != -1)
      .map(e -> e.getKey())
      .collect(Collectors.toList());
    Collections.shuffle(shuffledSubsequences);

    List<List<Tokenizer.Token>> sampledCappedSubsequences = new ArrayList<List<Tokenizer.Token>>();
    int totalNumTokens = 0;
    for (int i = 0; i < shuffledSubsequences.size(); i++) {
      if (ThreadLocalRandom.current().nextDouble() > keepProportion) {
        continue;
      }
      int numTokens = shuffledSubsequences
        .get(i)
        .stream()
        .mapToInt(t -> t.modelCodeRepresentation().split(" ").length)
        .sum();
      if (totalNumTokens + numTokens + 1 > maxSubsequencesSize) {
        break;
      }
      totalNumTokens += numTokens;
      sampledCappedSubsequences.add(shuffledSubsequences.get(i));
    }
    return sampledCappedSubsequences;
  }

  private int cappedPriorContextStart(List<Tokenizer.Token> priorContext, int maxContextSize) {
    return Math.max(0, priorContext.size() - maxContextSize);
  }

  public Map<List<Tokenizer.Token>, List<String>> alphaNumerics(
    List<Tokenizer.Token> priorContext,
    int maxContextSize
  ) {
    // end at the start of prior context that we feed to the model.
    priorContext = priorContext.subList(0, cappedPriorContextStart(priorContext, maxContextSize));

    // Cap the size of the context we consider. Mainly for performance reasons.
    if (priorContext.size() >= maxContextForAlphaSubsequences) {
      priorContext =
        priorContext.subList(
          priorContext.size() - maxContextForAlphaSubsequences,
          priorContext.size()
        );
      // make sure prior context doesn't start in the middle of an alphanumeric.
      while (
        priorContext.size() > 0 &&
        (
          priorContext.get(0) instanceof Tokenizer.AlphaToken ||
          priorContext.get(0) instanceof Tokenizer.NumberToken
        )
      ) {
        priorContext = priorContext.subList(1, priorContext.size());
      }
    }

    return alphaNumericFinder.alphaNumerics(priorContext);
  }

  public Input convert(
    List<Tokenizer.Token> priorContext,
    Map<List<Tokenizer.Token>, List<String>> alphaNumerics,
    List<String> words,
    int maxContextSize,
    double alphaSubsequenceProportion,
    Optional<AstParent> snippetContainer
  ) {
    List<List<Tokenizer.Token>> alphaSubsequencesContext = alphaSubsequencesContext(
      alphaNumerics,
      words,
      alphaSubsequenceProportion
    );

    return new Input(
      alphaSubsequencesContext,
      priorContext.subList(
        cappedPriorContextStart(priorContext, maxContextSize),
        priorContext.size()
      ),
      words,
      snippetContainer.map(c -> ((AstContainer) c).containerType())
    );
  }
}
