package core.ast.api;

import core.parser.ParseTree;
import core.util.Enclosures;
import core.util.Range;
import core.util.Whitespace;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultAstParent implements AstParent, Cloneable {

  protected List<AstNode> children = new ArrayList<>();
  protected Optional<AstParent> parent = Optional.empty();
  protected AstTree tree;

  // Metadata used during tree creation.
  private Optional<ParseTree> parseTree = Optional.empty();

  protected boolean tokenPresent(AstNode child) {
    return child.find(AstToken.class).findAny().isPresent();
  }

  protected boolean tokenPresentInOtherChildren(AstNode child) {
    return children().stream().filter(c -> c != child && tokenPresent(c)).findAny().isPresent();
  }

  protected void addChild(int index, AstNode child) {
    children().add(index, child);
    child.setParent(Optional.of(this));
  }

  protected void addChild(AstNode child) {
    addChild(children().size(), child);
  }

  protected void addChildUsingTokensPosition(AstNode child) {
    int index = 0;
    for (
      ;
      index < children().size() &&
      children()
        .get(index)
        .tokenRange()
        .map(r -> r.start < child.tokenRange().get().start)
        .orElse(true);
      index++
    ) {}

    addChild(index, child);
  }

  protected void addChildren(List<AstNode> children) {
    addChildren(this.children().size(), children);
  }

  protected void addChildren(int index, List<AstNode> children) {
    this.children().addAll(index, children);
    children.stream().forEach(child -> child.setParent(Optional.of(this)));
  }

  protected int setupSpacingBeforeBecomingVisible() {
    // Returns the token index for where to start inserting.
    // Delegate this to the parent if it exists, since it's better suited to manage whitespace
    // separation between its children.
    return parent()
      .map(parent -> parent.setupSpacingForChildBecomingVisible(this))
      .orElseGet(() -> tokenRangeWithCommentsAndWhitespace().start);
  }

  protected void removeSpacingBeforeBecomingInvisible() {
    parent().ifPresent(parent -> parent.removeSpacingForChildBecomingInvisible(this));
  }

  protected void removeAllChildren() {
    children().stream().forEach(child -> child.setParent(Optional.empty()));
    children().clear();
  }

  protected void removeChild(AstNode child) {
    children().remove(child);
    child.setParent(Optional.empty());
  }

  protected AstNode removeChild(int index) {
    AstNode ret = children().remove(index);
    ret.setParent(Optional.empty());
    return ret;
  }

  protected void setChild(AstNode oldChild, AstNode newChild) {
    newChild.setParent(oldChild.parent());
    children().set(children().indexOf(oldChild), newChild);
  }

  public <T> Optional<T> child(Class<T> type) {
    return children().stream().filter(type::isInstance).map(type::cast).findFirst();
  }

  public <T> Optional<T> child(Class<T> type, int skip) {
    return children().stream().filter(type::isInstance).map(type::cast).skip(skip).findFirst();
  }

  @Override
  public List<AstNode> children() {
    return children;
  }

  @Override
  public AstNode cloneTree(Optional<AstParent> parent, Map<AstToken, AstToken> oldToNewTokens) {
    try {
      AstParent ret = (AstParent) this.clone();
      ret.setParent(parent);
      ret.setChildren(
        ret
          .children()
          .stream()
          .map(c -> c.cloneTree(Optional.of(ret), oldToNewTokens))
          .collect(Collectors.toList())
      );
      return ret;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isToken(String code) {
    return false;
  }

  @Override
  public Optional<AstParent> parent() {
    return parent;
  }

  @Override
  public Optional<ParseTree> parseTree() {
    return parseTree;
  }

  @Override
  public void propagateOrFinalizeRemoval(AstNode child) {
    // Either propagate removal further, or to finalize the removal.
    // By default, propagate only if we have no children left.
    // This function could get overridden by something like a list implementation, which
    // does some sort of custom cleanup.
    if (!tokenPresentInOtherChildren(child)) {
      remove();
    } else {
      children().remove(child);
      tokens().removeAll(child.tokensInVisibleRange());
    }
  }

  @Override
  public Range range() {
    // if we have tokens in our subtree, use subtree to determine the range. Otherwise the range
    // is empty, and use tokens outside of it.
    Integer start = leftMost(AstToken.class)
      .map(token -> token.range().start)
      .orElseGet(() -> rightMostLeft(AstToken.class).map(token -> token.range().stop).orElse(0));
    Integer stop = rightMost(AstToken.class).map(token -> token.range().stop).orElse(start);
    return new Range(start, stop);
  }

  @Override
  public void removeSpacingForChildBecomingInvisible(AstNode child) {
    // this method is basically just the opposite of setupSpacingForChildBecomingVisible.
    Enclosures enclosures = new Enclosures();
    if (enclosures.insideListEnclosure(child)) {
      return;
    }

    // if there are visible elements to the left, consider deleting whitespace to the left.
    Range tokenRange = tokenRange().get();
    Range childTokenRange = child.tokenRange().get();
    if (
      tokenRange.start < childTokenRange.start &&
      tree().whitespace.isWhitespace(tokens().get(childTokenRange.start - 1))
    ) {
      tokens().remove(childTokenRange.start - 1);
    } else if (
      tokenRange.stop > childTokenRange.stop &&
      tree().whitespace.isWhitespace(tokens().get(childTokenRange.stop)) &&
      !(tokens().get(childTokenRange.stop) instanceof AstNewline)
    ) {
      // if there are visible elements to the right, consider deleting whitespace to the right.
      tokens().remove(childTokenRange.stop);
    } else {
      return;
    }
  }

  @Override
  public void setChildren(List<AstNode> children) {
    this.children = children;
    children.stream().forEach(child -> child.setParent(Optional.of(this)));
  }

  @Override
  public void setParent(Optional<AstParent> parent) {
    this.parent = parent;
  }

  @Override
  public void setParseTree(Optional<ParseTree> parseTree) {
    this.parseTree = parseTree;
  }

  @Override
  public void setTree(AstTree tree) {
    this.tree = tree;
  }

  @Override
  public int setupSpacingForChildBecomingVisible(AstNode child) {
    // Enclosures usually indicate that we're delimited properly, so just use the
    // end of it in case there's a comment in there or something.
    Enclosures enclosures = new Enclosures();
    if (enclosures.insideListEnclosure(child)) {
      return child.tokenRangeWithCommentsAndWhitespace().stop;
    }

    AstToken spaceToken = tree().factory.createToken(" ");

    // If we're the first visible thing, don't do anything.
    // Unclear if this case actually comes up.
    if (!tokenRange().isPresent()) {
      return child.tokenRangeWithCommentsAndWhitespace().stop;
    }

    // If there are visible tokens to the left, add a space where they end and insert after that.
    // We default to trying the left first because that seems to be often the case. E.g. return 5;
    if (tokenRange().get().start < child.tokenRangeWithCommentsAndWhitespace().start) {
      int tokenIndex = child.tokenRangeWithCommentsAndWhitespace().start;
      tokens().add(tokenIndex, spaceToken);
      return tokenIndex + 1;
    }

    // Otherwise they're to the right. Add a space to the right and insert before that.
    int tokenIndex = child.tokenRangeWithCommentsAndWhitespace().stop;
    tokens().add(tokenIndex, spaceToken);
    return tokenIndex;
  }

  @Override
  public List<AstToken> tokens() {
    return tree().tokens();
  }

  @Override
  public AstTree tree() {
    return tree;
  }

  @Override
  public String toDebugString(int level) {
    String result = "";

    for (int i = 0; i < level; i++) {
      result += "  ";
    }

    String type = parseTree.map(e -> e.getType()).orElse("");
    String astType = getClass().getTypeName().replace("core.ast.Ast$", "").replace("core.ast.", "");
    result += "<" + astType + (type.equals("") ? "" : ":" + type) + " " + range() + ">";

    if (children.size() > 0) {
      result +=
        "\n" +
        children.stream().map(e -> e.toDebugString(level + 1)).collect(Collectors.joining("\n")) +
        "\n";
      for (int i = 0; i < level; i++) {
        result += "  ";
      }
    }

    result += "</" + astType + ">";
    return result;
  }
}
