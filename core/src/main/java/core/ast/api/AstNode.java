package core.ast.api;

import com.google.common.collect.Lists;
import core.ast.Ast;
import core.parser.ParseTree;
import core.util.Range;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface AstNode {
  public List<AstNode> children();

  public AstNode cloneTree(Optional<AstParent> parent, Map<AstToken, AstToken> oldToNewTokens);

  // During construction of the AST, we sometimes need to check token content.
  // We cannot use .code() since the construction is not fully complete yet.
  public boolean isToken(String code);

  public Optional<AstParent> parent();

  public Optional<ParseTree> parseTree();

  public Range range();

  public void setParent(Optional<AstParent> parent);

  public void setParseTree(Optional<ParseTree> parseTree);

  public void setTree(AstTree tree);

  public AstTree tree();

  public List<AstToken> tokens();

  public String toDebugString(int level);

  @SuppressWarnings("unchecked")
  default <T> Optional<T> ancestor(Class<T> type) {
    // faster than using ancestors() because a list doesn't need to be allocated.
    // was actually a botteneck when we were calling this on every token.
    Optional<AstParent> parent = parent();
    while (parent.isPresent()) {
      if (type.isInstance(parent.get())) {
        return Optional.of((T) parent.get());
      }
      parent = parent.get().parent();
    }
    return Optional.empty();
  }

  default Stream<AstParent> ancestors() {
    List<AstParent> ancestors = new ArrayList<>();
    Optional<AstParent> parent = parent();
    while (parent.isPresent()) {
      ancestors.add(parent.get());
      parent = parent.get().parent();
    }

    return ancestors.stream();
  }

  default <T> Stream<T> ancestors(Class<T> type) {
    return ancestors().filter(type::isInstance).map(type::cast);
  }

  default String code() {
    return tokensInVisibleRange().stream().map(t -> t.code).collect(Collectors.joining(""));
  }

  default String codeWithCommentsAndWhitespace() {
    return tokensWithCommentsAndWhitespace()
      .stream()
      .map(t -> t.code)
      .collect(Collectors.joining(""));
  }

  default void detachParent() {
    parent()
      .ifPresent(
        parent -> {
          List<AstNode> children = new ArrayList<>(parent.children());
          children.remove(this);
          parent.setChildren(children);
        }
      );

    setParent(Optional.empty());
  }

  // filter nodes in the subtree. faster than using a stream of all the nodes.
  default Stream<AstNode> filter(Predicate<AstNode> f) {
    List<AstNode> nodes = new ArrayList<>();
    filter(nodes, this, f);
    return nodes.stream();
  }

  default void filter(List<AstNode> nodes, AstNode node, Predicate<AstNode> f) {
    if (f.test(node)) {
      nodes.add(node);
    }

    for (AstNode child : node.children()) {
      filter(nodes, child, f);
    }
  }

  default <T> Stream<T> find(Class<T> type) {
    return filter(e -> type.isAssignableFrom(e.getClass())).map(type::cast);
  }

  default Optional<AstToken> firstVisibleToken() {
    return leftMost(AstToken.class);
  }

  // apply function to each node in subtree. faster than using a stream of all the nodes.
  default void forEach(Consumer<AstNode> f) {
    forEach(this, f);
  }

  default void forEach(AstNode node, Consumer<AstNode> f) {
    f.accept(node);
    for (AstNode child : node.children()) {
      forEach(child, f);
    }
  }

  default Optional<AstToken> lastVisibleToken() {
    return rightMost(AstToken.class);
  }

  @SuppressWarnings("unchecked")
  default <T> Optional<T> leftMost(Class<T> type) {
    // in a traversal of the subtree, the left-most node of this type.
    for (AstNode child : children()) {
      Optional<T> result = child.leftMost(type);
      if (result.isPresent()) {
        return result;
      }
    }

    if (type.isInstance(this)) {
      return Optional.of((T) this);
    }

    return Optional.empty();
  }

  default <T> Optional<T> leftMostRight(Class<T> type) {
    // of the nodes to the right of this subtree (in a traversal), the left-most one of this
    // type.
    Optional<T> ret = rightSiblings()
      .stream()
      .map(right -> right.leftMost(type))
      .flatMap(Optional::stream)
      .findFirst();
    if (ret.isPresent()) {
      return ret;
    }

    return parent().flatMap(e -> e.leftMostRight(type));
  }

  default List<AstNode> leftSiblings() {
    return parent()
      .map(e -> e.children().subList(0, e.children().indexOf(this)))
      .orElse(Collections.<AstNode>emptyList());
  }

  default Optional<Range> lineRange() {
    return tokenRange().map(r -> tree().whitespace.lineRange(r));
  }

  @SuppressWarnings("unchecked")
  default <T> Optional<T> rightMost(Class<T> type) {
    // in a traversal of the subtree, the right-most node of this type.
    List<AstNode> children = children();
    for (int i = children.size() - 1; i >= 0; i--) {
      AstNode child = children.get(i);
      Optional<T> result = child.rightMost(type);
      if (result.isPresent()) {
        return result;
      }
    }

    if (type.isInstance(this)) {
      return Optional.of((T) this);
    }

    return Optional.empty();
  }

  default <T> Optional<T> rightMostLeft(Class<T> type) {
    // of the nodes to the left of this subtree (in a traversal), the right most one of this
    // type.
    Optional<T> ret = Lists
      .reverse(leftSiblings())
      .stream()
      .map(left -> left.rightMost(type))
      .flatMap(Optional::stream)
      .findFirst();
    if (ret.isPresent()) {
      return ret;
    }

    return parent().flatMap(e -> e.rightMostLeft(type));
  }

  default List<AstNode> rightSiblings() {
    return parent()
      .map(
        e -> {
          List<AstNode> children = e.children();
          return children.subList(children.indexOf(this) + 1, children.size());
        }
      )
      .orElse(Collections.<AstNode>emptyList());
  }

  default <T extends AstNode> T replace(T replacement) {
    // assumes we're non-empty for now.
    AstParent p = parent().get();
    int start =
      this.tokenRange()
        .map(range -> range.start)
        .orElse(this.tokenRangeWithCommentsAndWhitespace().start);
    tokens().removeAll(this.tokensInVisibleRange());
    tokens().addAll(start, replacement.tokensInVisibleRange());
    replacement.forEach(t -> t.setTree(tree()));

    List<AstNode> children = new ArrayList<>(p.children());
    children.set(children.indexOf(this), replacement);
    p.setChildren(children);
    tree().updateComments();
    return replacement;
  }

  default void remove() {
    parent().ifPresent(e -> e.propagateOrFinalizeRemoval(this));
  }

  default Range rangeWithCommentsAndWhitespace() {
    Range range = tokenRangeWithCommentsAndWhitespace();
    if (tree().tokens().size() == 0) {
      return new Range(0, 0);
    }
    return new Range(
      tree().tokens().get(range.start).range.start,
      tree().tokens().get(range.stop - 1).range.stop
    );
  }

  default Optional<Ast.Identifier> name() {
    return children().stream().flatMap(e -> identifier(e).stream()).findFirst();
  }

  default String nameString() {
    return name().map(e -> e.code()).orElse("");
  }

  default Optional<Ast.Identifier> identifier(AstNode e) {
    if (e instanceof Ast.Identifier) {
      return Optional.of((Ast.Identifier) e);
    } else if (e.children().size() == 1) {
      return identifier(e.children().get(0));
    }
    return Optional.empty();
  }

  // this isn't called visibleTokens because it includes tokens in between the visible tokens
  default List<AstToken> tokensInVisibleRange() {
    return tokenRange()
      .map(range -> tree().tokens().subList(range.start, range.stop))
      .orElse(Collections.emptyList());
  }

  default String toDebugString() {
    return toDebugString(0);
  }

  default Optional<Range> tokenRange() {
    return firstVisibleToken()
      .map(first -> new Range(first.index, lastVisibleToken().get().index + 1));
  }

  default Range tokenRangeWithCommentsAndWhitespace() {
    return new Range(
      rightMostLeft(AstToken.class).map(t -> t.index + 1).orElse(0),
      leftMostRight(AstToken.class).map(t -> t.index).orElse(tree().tokens().size())
    );
  }

  default List<AstToken> tokensWithCommentsAndWhitespace() {
    Range range = tokenRangeWithCommentsAndWhitespace();
    return tree().tokens().subList(range.start, range.stop);
  }
}
