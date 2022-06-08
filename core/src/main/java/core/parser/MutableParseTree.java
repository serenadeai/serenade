package core.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// MutableParseTree type that's easier to work with when sampling transcript parsing trees because it doesn't
// require the full source to be generated up front. Production can probably still use ParseTree.
public class MutableParseTree {

  public final String type;
  public Optional<MutableParseTree> parent = Optional.empty();
  private List<MutableParseTree> children = Collections.emptyList();
  public final String text;

  public MutableParseTree(String type) {
    this.type = type;
    this.text = "";
  }

  public MutableParseTree(String type, String text) {
    this.type = type;
    this.text = text;
  }

  public List<MutableParseTree> children() {
    return children;
  }

  public MutableParseTree setChildren(List<MutableParseTree> children) {
    this.children = children;
    for (MutableParseTree child : children) {
      child.parent = Optional.of(this);
    }
    return this;
  }

  private Optional<ParseTree> toParseTree(String source, int startIndex) {
    if (!this.text.equals("")) {
      return Optional.of(
        new ParseTree(
          this.type,
          "",
          source,
          startIndex,
          this.text.length() + startIndex,
          Optional.empty()
        )
      );
    }
    int nextStartIndex = startIndex;
    List<ParseTree> convertedChildren = new ArrayList<>();
    for (MutableParseTree child : this.children()) {
      Optional<ParseTree> convertedChild = child.toParseTree(source, nextStartIndex);
      if (convertedChild.isPresent()) {
        convertedChildren.add(convertedChild.get());
        nextStartIndex = convertedChild.get().getStop() + 1; // Space between words
      }
    }
    if (convertedChildren.size() == 0) {
      return Optional.empty();
    }
    ParseTree result = new ParseTree(
      this.type,
      this.text,
      source,
      convertedChildren.get(0).getStart(),
      convertedChildren.get(convertedChildren.size() - 1).getStop(),
      Optional.empty()
    );
    result.setChildren(convertedChildren);
    return Optional.of(result);
  }

  private List<MutableParseTree> tokens() {
    if (!text.equals("")) {
      return Arrays.asList(this);
    } else {
      return children.stream().flatMap(c -> c.tokens().stream()).collect(Collectors.toList());
    }
  }

  public ParseTree toParseTree() {
    String source = tokens().stream().map(t -> t.text).collect(Collectors.joining(" "));
    return toParseTree(source, 0).orElse(new ParseTree(type, "", "", 0, 0, Optional.empty()));
  }
}
