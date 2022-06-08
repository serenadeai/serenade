package corpusgen.mapping;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import core.codeengine.Tokenizer;
import core.util.Range;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Mapping {

  public List<String> phrases;
  public List<Tokenizer.Token> outputTokens;
  public List<Range> remaining;
  public boolean partial;

  public Mapping(
    List<String> phrases,
    List<Tokenizer.Token> outputTokens,
    List<Range> remaining,
    boolean partial
  ) {
    this.phrases = new ArrayList<>(phrases);
    this.outputTokens = new ArrayList<>(outputTokens);
    this.remaining = new ArrayList<>(remaining);
    this.partial = partial;
  }

  public Mapping(List<Range> remaining, boolean partial) {
    this(Collections.emptyList(), Collections.emptyList(), remaining, partial);
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("phrases", phrases.stream().collect(Collectors.joining(" ")))
      .add(
        "outputTokens",
        outputTokens.stream().map(t -> t.modelCodeRepresentation()).collect(Collectors.joining(" "))
      )
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof Mapping)) {
      return false;
    }
    Mapping m = (Mapping) o;

    return (phrases.equals(m.phrases) && outputTokens.equals(m.outputTokens));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(phrases, outputTokens);
  }
}
