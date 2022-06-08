package core.ast.api;

import core.util.Range;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AstTrailingClauseList<T extends AstParent> extends AstList<T> {

  @Override
  public boolean boundedBelow() {
    return false;
  }

  protected boolean newlineSpacing() {
    // First element is on a different line from the bound above.
    return (
      elements().size() == 0 ||
      (
        tree().whitespace.lineFromTokenIndex(tokenRangeWithCommentsAndWhitespace().start) !=
        tree().whitespace.lineFromTokenIndex(elements().get(0).tokenRange().get().start)
      )
    );
  }

  @Override
  public void addAtLine(int line, AstParent node) {
    if (!newlineSpacing()) {
      int elementIndex = elements()
        .stream()
        .filter(e -> e.lineRange().get().start == line)
        .findFirst()
        .map(e -> elements().indexOf(e))
        .orElse(elements().size());
      indentationUtils.increaseIndentation(node, indentationUtils.indent(this));
      while (tree().whitespace.isWhitespace(node.tokens().get(0))) {
        node.tokens().remove(0);
      }
      add(elementIndex, node);
      return;
    }
    super.addAtLine(line, node);
  }

  @Override
  public List<Integer> availableLines() {
    if (!newlineSpacing()) {
      List<Integer> result = elements()
        .stream()
        .map(e -> e.lineRange().get().start)
        .collect(Collectors.toCollection(ArrayList::new));
      if (elements().size() > 0) {
        result.add(elements().get(elements().size() - 1).lineRange().get().stop);
      }
      return result;
    }
    return super.availableLines();
  }

  @Override
  public boolean isMultiline() {
    return true;
  }
}
