package core.util;

import core.ast.api.AstNode;
import core.ast.api.AstToken;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Enclosures {

  // FormattedTextGenerator has something similar, but uses a more liberal definition. Perhaps we
  // should consolidate.
  private final Map<String, String> enclosureStartToEnd;
  private final Map<String, String> enclosureEndToStart;

  public Enclosures() {
    enclosureStartToEnd = new HashMap<String, String>();
    enclosureStartToEnd.put("(", ")");
    enclosureStartToEnd.put("[", "]");
    enclosureStartToEnd.put("{", "}");
    enclosureStartToEnd.put("<", ">");
    enclosureStartToEnd.put("'", "'");
    enclosureStartToEnd.put("\"", "\"");
    enclosureEndToStart =
      enclosureStartToEnd
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
  }

  public Collection<String> starts() {
    return enclosureStartToEnd.keySet();
  }

  public Collection<String> ends() {
    return enclosureStartToEnd.values();
  }

  public Optional<String> startToEnd(String start) {
    return Optional.ofNullable(enclosureStartToEnd.get(start));
  }

  public Optional<String> endToStart(String end) {
    return Optional.ofNullable(enclosureEndToStart.get(end));
  }

  public boolean insideListEnclosure(AstNode child) {
    return child
      .rightMostLeft(AstToken.class)
      .flatMap(
        leftToken ->
          startToEnd(leftToken.code)
            .flatMap(
              enclosureEnd ->
                child
                  .leftMostRight(AstToken.class)
                  .map(rightNode -> enclosureEnd.equals(rightNode.code))
            )
      )
      .orElse(false);
  }
}
