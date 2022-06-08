package core.ast.api;

import core.util.Range;

// Handles else addition for Python. Adds element on the next line. Extra newline removal
// is handled by default behavior of stripping whitespace to the left
public abstract class AstMultilineOptional<T extends AstParent> extends AstOptional<T> {

  private IndentationUtils indentationUtils = new IndentationUtils();

  @Override
  protected int setupSpacingBeforeBecomingVisible() {
    // Insert right after the first newline available. We use this heuristic since
    // we're often tagging on to the end of something, like a try or an if.
    // The current default if there isn't a newline is mostly arbitrary. Perhaps we
    // should add one instead.

    int indexRelativeToNewline = isInlineWithSiblings() ? 0 : 1;
    return tokensWithCommentsAndWhitespace()
      .stream()
      .filter(AstNewline.class::isInstance)
      .findFirst()
      .map(t -> tokens().indexOf(t) + indexRelativeToNewline)
      .orElse(tokenRangeWithCommentsAndWhitespace().stop);
  }

  protected boolean isInlineWithSiblings() {
    return false;
  }

  @Override
  protected void prepareForAdd(AstNode e) {
    if (isInlineWithSiblings()) {
      e.tokens().add(0, tree().factory.createToken(" "));
    } else {
      indentationUtils.increaseIndentation(e, indentationUtils.indent(this));
      e.tokens().add(tree().factory.createNewline());
    }
  }

  @Override
  public void propagateOrFinalizeRemoval(AstNode child) {
    Range removalTokenRange = child.tokenRange().get();
    removalTokenRange.start = tree().whitespace.expandLeftOnLine(removalTokenRange.start);
    removalTokenRange.stop = tree().whitespace.expandRightOnLine(removalTokenRange.stop);
    if (tokens().get(removalTokenRange.start - 1) instanceof AstNewline) {
      removalTokenRange.stop++;
    }

    tokens().removeAll(tokens().subList(removalTokenRange.start, removalTokenRange.stop));
    removeChild(child);
  }
}
