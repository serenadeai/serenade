package replayer;

import java.util.List;
import speechengine.gen.rpc.AlternativesResponse;

public class Prediction {

  public final AlternativesResponse alternatives;
  public final List<String> transcripts;
  public final List<Double> acousticCosts;
  public final List<Double> languageModelCosts;

  Prediction(
    AlternativesResponse alternatives,
    List<String> transcripts,
    List<Double> acousticCosts,
    List<Double> languageModelCosts
  ) {
    this.alternatives = alternatives;
    this.transcripts = transcripts;
    this.acousticCosts = acousticCosts;
    this.languageModelCosts = languageModelCosts;
  }
}
