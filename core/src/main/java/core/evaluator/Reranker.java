package core.evaluator;

import core.codeengine.SlotContext;
import core.evaluator.CustomCommandEvaluator;
import core.gen.rpc.CommandType;
import core.gen.rpc.CustomCommand;
import core.metadata.CommandsResponseAlternativeWithMetadata;
import core.metadata.EditorStateWithMetadata;
import core.snippet.SnippetTrigger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import speechengine.gen.rpc.Alternative;
import toolbelt.logging.Logs;

@Singleton
public class Reranker {

  private static Logger logger = LoggerFactory.getLogger(Reranker.class);

  @Inject
  CustomCommandEvaluator customCommandEvaluator;

  // Filters out invalid alternatives that are < 5% as likely as the top.
  private final double invalidAlternativeRelativeCostThreshold = 3.0;
  private final double nonWildcardCustomCommandLanguageModelCost = 5.5;
  // Filters out alternatives that are < 1.1% as likely as the top.
  private final double parsedTranscriptRelativeCostThreshold = 4.5;
  // Doesn't do much since the beam in speech engine is smaller.
  private final double transcriptRelativeCostThreshold = 8.0;
  private final double wildcardCustomCommandBoost = 5.0;

  @Inject
  public Reranker() {}

  private Alternative adjustCosts(Alternative alternative, EditorStateWithMetadata state) {
    double languageModelCost = alternative.getLanguageModelCost();

    Optional<SnippetTrigger> trigger = customCommandEvaluator.trigger(
      alternative.getTranscript(),
      state
    );

    if (trigger.isPresent()) {
      if (trigger.get().wildcard()) {
        languageModelCost -= wildcardCustomCommandBoost;
      } else {
        languageModelCost = nonWildcardCustomCommandLanguageModelCost;
      }
    }

    return Alternative
      .newBuilder(alternative)
      .setCost(alternative.getAcousticCost() + languageModelCost)
      .setLanguageModelCost(languageModelCost)
      .build();
  }

  private <T> List<T> sortAndFilter(
    List<T> alternatives,
    Function<T, Double> cost,
    double relativeCostThreshold
  ) {
    List<T> result = new ArrayList<>(alternatives);
    double firstCost = result.stream().findFirst().map(cost).orElse(0.0);
    Collections.sort(result, Comparator.comparing(cost));
    result =
      result
        .stream()
        .filter(a -> cost.apply(a) < firstCost + relativeCostThreshold)
        .collect(Collectors.toList());
    return result;
  }

  public List<CommandsResponseAlternativeWithMetadata> rerankEvaluated(
    List<CommandsResponseAlternativeWithMetadata> alternatives
  ) {
    // apply a harsher filter to invalid alternatives.
    double firstCost = alternatives
      .stream()
      .findFirst()
      .map(
        alternative ->
          alternative.parsed.alternative.getCost() + alternative.parsed.parseCost.orElse(0.0)
      )
      .orElse(0.0);
    alternatives =
      alternatives
        .stream()
        .filter(
          alternative ->
            alternative.commands.get(0).getType() != CommandType.COMMAND_TYPE_INVALID ||
            alternative.parsed.alternative.getCost() +
            alternative.parsed.parseCost.orElse(0.0) <
            firstCost +
            invalidAlternativeRelativeCostThreshold
        )
        .collect(Collectors.toList());

    // Reranking pass that re-orders auto-styled alternatives using the auto-style scores and the
    // contextual language model scores. Doesn't bother cap anything since it won't effect an auto-execution
    // decision.
    List<CommandsResponseAlternativeWithMetadata> alternativesToSort = new ArrayList<>(
      alternatives.stream().filter(a -> a.autoStyleCost.isPresent()).collect(Collectors.toList())
    );

    Collections.sort(
      alternativesToSort,
      Comparator.comparingDouble(
        a ->
          a.parsed.alternative.getAcousticCost() +
          a.autoStyleCost.get() +
          a.contextualLanguageModelCost.orElse(a.parsed.alternative.getLanguageModelCost())
      )
    );
    int nextToReplace = 0;
    for (int i = 0; i < alternatives.size() && nextToReplace < alternativesToSort.size(); i++) {
      if (alternatives.get(i).autoStyleCost.isPresent()) {
        alternatives.set(i, alternativesToSort.get(nextToReplace++));
      }
    }

    return alternatives;
  }

  public List<ParsedTranscript> rerankParsedTranscripts(List<ParsedTranscript> parsed) {
    // Reranks results after they come back from the transcript parser model. Uses a more aggressive
    // score threshold to filter out unlikely transcripts and unlikely parses of likely transcripts.
    return sortAndFilter(
      parsed,
      p -> p.alternative.getCost() + p.parseCost.orElse(0.0),
      parsedTranscriptRelativeCostThreshold
    );
  }

  public List<Alternative> rerankTranscripts(
    List<Alternative> alternatives,
    EditorStateWithMetadata state
  ) {
    // Reranks results when they come back on the speech engine. In particular:
    // - Filters out everything if the empty transcript came first.
    // - Adjusts the costs of the alternatives, i.e. boosts custom commands.
    // - Sorts by the initial score, with a liberal (possibly unecessary) score threshold.
    if (alternatives.stream().findFirst().map(a -> a.getTranscript().equals("")).orElse(false)) {
      return Collections.emptyList();
    }
    return sortAndFilter(
      alternatives
        .stream()
        .map(a -> adjustCosts(a, state))
        .filter(a -> !a.getTranscript().equals(""))
        .collect(Collectors.toList()),
      a -> a.getCost(),
      transcriptRelativeCostThreshold
    );
  }
}
