package core.codeengine;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import core.ast.api.AstParent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Input {

  public final List<List<Tokenizer.Token>> alphaSubsequenceContext;
  public final List<Tokenizer.Token> leadingContext;
  public final List<String> phrases;
  public final Optional<String> snippetContainer;

  public Input(
    List<List<Tokenizer.Token>> alphaSubsequenceContext,
    List<Tokenizer.Token> leadingContext,
    List<String> phrases,
    Optional<String> snippetContainer
  ) {
    this.alphaSubsequenceContext = alphaSubsequenceContext;
    this.leadingContext = leadingContext;
    this.phrases = phrases;
    this.snippetContainer = snippetContainer;
  }

  public String modelCodeRepresentation() {
    List<Tokenizer.Token> tokens = new ArrayList<Tokenizer.Token>();
    alphaSubsequenceContext
      .stream()
      .forEach(
        t -> {
          tokens.addAll(t);
          tokens.add(new Tokenizer.AlphaSubsequenceDelimToken());
        }
      );
    if (tokens.size() > 0) {
      tokens.remove(tokens.size() - 1);
    }
    tokens.add(new Tokenizer.ContextStartToken());
    tokens.addAll(leadingContext);
    if (snippetContainer.isPresent()) {
      tokens.add(new Tokenizer.SnippetContainerToken(snippetContainer.get()));
    } else {
      tokens.add(new Tokenizer.EnglishStartToken());
    }
    return (
      tokens.stream().map(t -> t.modelCodeRepresentation()).collect(Collectors.joining(" ")) +
      " " +
      phrases.stream().collect(Collectors.joining(" "))
    );
  }

  public String leadingContextRepresentation() {
    return leadingContext
      .stream()
      .map(t -> t.modelCodeRepresentation())
      .collect(Collectors.joining(" "));
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("phrases", phrases)
      .add("leadingContext", leadingContext)
      .add("alphaSubsequenceContext", alphaSubsequenceContext)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof Input)) {
      return false;
    }
    Input m = (Input) o;

    return (
      m.alphaSubsequenceContext.equals(alphaSubsequenceContext) &&
      m.phrases.equals(phrases) &&
      m.leadingContext.equals(leadingContext)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.alphaSubsequenceContext, this.phrases, this.leadingContext);
  }
}
