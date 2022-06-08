package corpusgen.mapping;

import com.google.common.base.MoreObjects;
import core.ast.api.AstNode;
import core.codeengine.Input;
import core.codeengine.InputConverter;
import core.codeengine.Tokenizer;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class FullContextMapping {

  protected InputConverter inputConverter;

  public final Mapping mapping;
  public final Tokens tokens;
  public final int start;
  public final Optional<AstNode> node;

  @AssistedInject
  public FullContextMapping(
    InputConverter inputConverter,
    @Assisted Mapping mapping,
    @Assisted Tokens tokens,
    @Assisted int start,
    @Assisted Optional<AstNode> node
  ) {
    this.inputConverter = inputConverter;
    this.mapping = mapping;
    this.tokens = tokens;
    this.start = start;
    this.node = node;
  }

  @AssistedFactory
  public interface Factory {
    FullContextMapping create(Mapping mapping, Tokens tokens, int start, Optional<AstNode> node);
  }

  public Input sampleInput() {
    // We sample various context sizes. The main rationale here is so that we can produce code
    // when there isn't much in the file. For small context sizes, we drop the subsequence context
    // since you won't have that when you're forced to use a small context. We also sample larger
    // context sizes so that our average context size is what we see in production, and there's
    // possibly some generalization benefit to varying this.
    //
    // We drop subsequences with 10% probability. We've tried 50% and 100%. The former didn't
    // carry much weight because it could only penalize competing styles by ~50%. The latter
    // inferred a lot from the lack of a style appearing, which often in an incomplete file but
    // not fully completed files. We may have to revisit this if there's still an issue with 90%.
    int contextSize = ThreadLocalRandom.current().nextInt(50);
    // We mask the subsequences at 25 so that it's not on the border with inference time.
    // This actually had a meaningful effect on unit tests.
    double subsequenceKeepProportion = contextSize >= 25 ? 0.9 : 0.0;
    List<Tokenizer.Token> previousTokens = tokens.list.subList(
      0,
      start == 0 ? 0 : tokens.tokenPosition(start)
    );
    return inputConverter.convert(
      previousTokens,
      inputConverter.alphaNumerics(previousTokens, contextSize),
      mapping.phrases,
      contextSize,
      subsequenceKeepProportion,
      node.flatMap(n -> n.parent())
    );
  }

  public String outputModelCodeRepresentation() {
    return mapping.outputTokens
      .stream()
      .map(t -> t.modelCodeRepresentation())
      .collect(Collectors.joining(" "));
  }

  public String transcript() {
    return mapping.phrases.stream().collect(Collectors.joining(" "));
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("input", transcript())
      .add("output", outputModelCodeRepresentation())
      .toString();
  }
}
