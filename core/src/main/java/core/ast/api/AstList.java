package core.ast.api;

import core.ast.Ast;
import core.util.Range;
import core.util.Whitespace;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AstList<T extends AstParent>
  extends DefaultAstParent
  implements AstContainer {

  protected IndentationUtils indentationUtils = new IndentationUtils();
  protected Whitespace whitespace = new Whitespace();

  public abstract String containerType();

  public abstract Class<? extends T> elementType();

  public List<Class<? extends AstParent>> innerElementTypes() {
    return Arrays.asList();
  }

  private Optional<AstParent> currentPlaceholder() {
    Optional<AstParent> currentPlaceholder = Optional.empty();
    if (elements().size() == 1) {
      AstParent element = elements().get(0);
      Optional<AstParent> placeholder = placeholder();
      if (
        placeholder.isPresent() &&
        placeholder.get().getClass().isInstance(element) &&
        placeholder.get().code().equals(element.code())
      ) {
        currentPlaceholder = Optional.of(element);
      }
    }
    return currentPlaceholder;
  }

  public void addAtLine(int line, AstParent e) {
    AstParent elementToAdd = e;
    try {
      if (!(e instanceof Ast.Comment) && !elementType().isInstance(e)) {
        AstParent wrappedElement = elementType().getDeclaredConstructor().newInstance();
        e.setParent(Optional.of(wrappedElement));
        wrappedElement.setTree(e.tree());
        wrappedElement.setChildren(Arrays.asList(e));
        elementToAdd = wrappedElement;
      }
    } catch (
      NoSuchMethodException
      | InstantiationException
      | IllegalAccessException
      | InvocationTargetException error
    ) {
      throw new RuntimeException("Error instantiating " + elementType(), error);
    }

    Optional<AstParent> currentPlaceholder = currentPlaceholder();
    if (insideLineInterior()) {
      setupLineBelow();
      line = availableLines().get(availableLines().size() - 1);
    }
    if (!availableLines().contains(line)) {
      throw new RuntimeException("Tried to add at a line that wasn't available");
    }

    indentationUtils.increaseIndentation(e, indentationUtils.indent(this));

    tree().addTokensAtLine(line, elementToAdd);
    if (!(elementToAdd instanceof Ast.Comment)) {
      addChildUsingTokensPosition(elementToAdd);
      int index = elements().indexOf(elementToAdd);
      if (elements().size() > 1) {
        AstToken delimiter = tree().factory.createToken(delimiter());
        if (index == elements().size() - 1) {
          tokens().add(elements().get(index - 1).tokenRange().get().stop, delimiter);
        } else {
          tokens().add(elementToAdd.tokenRange().get().stop, delimiter);
        }
        addChildUsingTokensPosition(delimiter);
      }
    }
    tree().updateComments();
    currentPlaceholder.ifPresent(p -> p.remove());
  }

  public String delimiter() {
    return "";
  }

  public boolean canAdd(AstNode element) {
    return (
      elementType().isInstance(element) ||
      innerElementTypes().stream().anyMatch(t -> t.isInstance(element))
    );
  }

  public boolean canAdd(Class<? extends AstNode> nodeType) {
    return (
      elementType().isAssignableFrom(nodeType) ||
      innerElementTypes().stream().anyMatch(t -> t.isAssignableFrom(nodeType))
    );
  }

  public boolean isMultiline() {
    return false;
  }

  private List<AstToken> whitespace() {
    // previously we were matching whitespace here, perhaps we could introduce that again.
    return Arrays.asList(tree().factory.createToken(" "));
  }

  public void add(int index, AstParent e) {
    Optional<AstParent> currentPlaceholder = currentPlaceholder();
    List<AstNode> newChildren = new ArrayList<>();
    List<AstToken> newTokens = new ArrayList<>();
    AstToken delimiter = tree().factory.createToken(delimiter());
    List<AstToken> whitespace = whitespace();
    int tokenIndex;
    int childIndex = 0;
    if (index > 0) {
      // insert at the end of the previous child, including any trailing whitespace and comments
      AstParent previous = elements().get(index - 1);
      childIndex = children().indexOf(previous) + 1;
      tokenIndex = previous.tokenRange().get().stop;

      newTokens.add(delimiter);
      newTokens.addAll(whitespace);
      newTokens.addAll(e.tokensInVisibleRange());

      newChildren.add(delimiter);
      newChildren.add(e);
    } else if (elements().size() > 0) {
      tokenIndex = tokenRange().get().start;

      newTokens.addAll(e.tokensInVisibleRange());
      newTokens.add(delimiter);
      newTokens.addAll(whitespace);

      newChildren.add(e);
      newChildren.add(delimiter);
    } else {
      tokenIndex = setupSpacingBeforeBecomingVisible();
      newTokens.addAll(e.tokensInVisibleRange());
      newChildren.add(e);
    }

    tokens().addAll(tokenIndex, newTokens);
    addChildren(childIndex, newChildren);
    forEach(t -> t.setTree(tree()));
    currentPlaceholder.ifPresent(p -> p.remove());
  }

  public List<Integer> availableLines() {
    Range range = availableLineRange();
    List<Range> visibleLineRanges = children()
      .stream()
      .map(c -> tree().whitespace.lineRange(c.tokenRange().get()))
      .collect(Collectors.toList());

    // Only consider line starts belonging to this list and not its children.
    List<Integer> lines = new ArrayList<>();
    for (int i = range.start; i < range.stop; i++) {
      int index = i;
      if (visibleLineRanges.stream().anyMatch(r -> r.start < index && index < r.stop)) {
        continue;
      }

      lines.add(i);
    }
    return lines;
  }

  public boolean boundedBelow() {
    return true;
  }

  public boolean boundedAbove() {
    return true;
  }

  protected void clearWithoutNewline() {
    Range tokenRange = tokenRangeWithCommentsAndWhitespace();
    setChildren(new ArrayList<>());
    tokens().removeAll(tokens().subList(tokenRange.start, tokenRange.stop));
  }

  private boolean insideLineInterior() {
    Range tokenRange = tokenRangeWithCommentsAndWhitespace();
    Range lineRange = tree().whitespace.lineRange(tokenRange);
    return (
      lineRange.start + 1 == lineRange.stop &&
      tokenRange.start > 0 &&
      tokenRange.stop < tokens().size()
    );
  }

  protected Optional<AstParent> placeholder() {
    return Optional.empty();
  }

  private void addPlaceholderIfEmpty() {
    Optional<AstParent> placeholder = placeholder();
    if (elements().size() == 0 && placeholder.isPresent()) {
      addAtLine(availableLines().get(0), placeholder.get());
    }
  }

  protected boolean prefixPlaceholder() {
    // things like cls or self that stay when you add an element.
    return false;
  }

  protected void setupLineBelow() {
    // Remove leading/trailing whitespace.
    while (
      tokenRangeWithCommentsAndWhitespace().start != tokenRangeWithCommentsAndWhitespace().stop &&
      tree().whitespace.isWhitespace(tokens().get(tokenRangeWithCommentsAndWhitespace().start))
    ) {
      tokens().remove(tokenRangeWithCommentsAndWhitespace().start);
    }
    while (
      tokenRangeWithCommentsAndWhitespace().start != tokenRangeWithCommentsAndWhitespace().stop &&
      tree().whitespace.isWhitespace(tokens().get(tokenRangeWithCommentsAndWhitespace().stop - 1))
    ) {
      tokens().remove(tokenRangeWithCommentsAndWhitespace().stop - 1);
    }

    // if there's a body, indent it on its own line.
    if (tokenRangeWithCommentsAndWhitespace().start != tokenRangeWithCommentsAndWhitespace().stop) {
      tokens()
        .addAll(
          tokenRangeWithCommentsAndWhitespace().start,
          Arrays.asList(
            tree().factory.createNewline(),
            tree().factory.createToken(indentationUtils.indent(this))
          )
        );
    }

    // Indent closing brace or tag.
    tokens()
      .addAll(
        tokenRangeWithCommentsAndWhitespace().stop,
        Arrays.asList(
          tree().factory.createNewline(),
          tree().factory.createToken(indentationUtils.parentIndent(this))
        )
      );
  }

  public Range availableLineRange() {
    // Line below needs setup. e.g. if (true) {}. Currently not working/needed for unbounded above.
    if (insideLineInterior()) {
      int line = tree().whitespace.lineFromTokenIndex(tokenRangeWithCommentsAndWhitespace().start);
      return new Range(line + 1, line + 2);
    }

    Range tokenRange = tokenRangeWithCommentsAndWhitespace();
    // All !boundedAbove are decorator lists. Make sure the decorators are contiguous.
    if (!boundedAbove()) {
      tokenRange.start = tokenRange().map(r -> r.start).orElse(tokenRange.stop);
    }

    Range lineRange = tree().whitespace.lineRange(tokenRange);
    if (!boundedAbove()) {
      return lineRange;
    }

    // You can't insert stuff on the first line, because it'll be the "{" or ":", but you can if
    // this is a top-level list and nothing is before you.
    if (tokenRange.start != 0) {
      lineRange.start++;
    }

    // Allow us to add a line at the end of the file.
    if (tokenRange.stop == tokens().size()) {
      lineRange.stop++;
    }

    return lineRange;
  }

  public void clear() {
    Range tokenRange = tokenRangeWithCommentsAndWhitespace();
    if (isMultiline()) {
      if (!boundedBelow()) { // e.g. python statement lists
        tokenRange.stop = tokenRange().map(r -> r.stop).orElse(tokenRange.start);
      } else if (!boundedAbove()) { // e.g. decorator lists
        tokenRange.start = tokenRange().map(r -> r.start).orElse(tokenRange.stop);
      }

      setChildren(new ArrayList<>());
      tokens().removeAll(tokens().subList(tokenRange.start, tokenRange.stop));

      // e.g. regular brace-enclosed statement lists.
      if (boundedBelow() && boundedAbove()) {
        // Indent closing brace or tag.
        tokens()
          .addAll(
            tokenRange.start,
            Arrays.asList(
              tree().factory.createNewline(),
              tree().factory.createToken(indentationUtils.parentIndent(this))
            )
          );
      }
    } else {
      clearWithoutNewline();
    }
    if (parent().filter(p -> p instanceof AstListOptional).isPresent()) {
      remove();
    }
    addPlaceholderIfEmpty();
  }

  @SuppressWarnings("unchecked")
  public List<T> elements() {
    return children()
      .stream()
      .filter(elementType()::isInstance)
      .map(e -> (T) e)
      .collect(Collectors.toList());
  }

  @Override
  public void propagateOrFinalizeRemoval(AstNode child) {
    // we don't propagate the removal upwards when we are out of children
    if (canAdd(child)) {
      remove(child);
      return;
    }

    removeChild(child);
  }

  public void remove(AstNode element) {
    if (elements().size() == 1) {
      if (parent().filter(p -> p instanceof AstListOptional).isPresent()) {
        remove();
        return;
      }
    }
    removeDelimiter(element);

    Range tokenRange = element.tokenRange().get();
    Range removalRange = tree().whitespace.includeLineWhitespaceOnOneSide(tokenRange);

    List<AstToken> tokens = new ArrayList<>(tokens().subList(tokenRange.start, tokenRange.stop));
    tokens().removeAll(tokens().subList(removalRange.start, removalRange.stop));
    tree().updateComments();
    removeChild(element);

    AstTree.attachTree(tree().factory, element, Collections.emptyList(), tokens);
    addPlaceholderIfEmpty();
  }

  protected void removeDelimiter(AstNode element) {
    if (delimiter().equals("")) {
      return;
    }

    AstToken delimiter;
    int index = children().indexOf(element);
    if (index > 0) {
      delimiter = (AstToken) children().get(index - 1);
    } else if (children().size() > 1) {
      delimiter = (AstToken) children().get(index + 1);
    } else {
      return;
    }

    tokens().remove(delimiter);
    removeChild(delimiter);
  }
}
