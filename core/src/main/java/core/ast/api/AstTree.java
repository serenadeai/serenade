package core.ast.api;

import core.ast.Ast;
import core.ast.AstFactory;
import core.util.Range;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AstTree {

  private final AstTokens tokens;

  public final List<Ast.Comment> comments;
  public final AstFactory factory;
  public final AstNode root;
  public final AstWhitespace whitespace;
  public Optional<AstSyntaxError> syntaxError = Optional.empty();

  public AstTree(
    AstFactory factory,
    AstNode root,
    List<Ast.Comment> comments,
    List<AstToken> tokens
  ) {
    this.factory = factory;
    this.root = root;
    this.comments = comments;
    this.tokens = new AstTokens(this, tokens);
    this.whitespace = new AstWhitespace(this.tokens);
  }

  public void updateInvariants() {
    tokens.updateInvariants();
  }

  public static AstTree attachTree(
    AstFactory factory,
    AstNode root,
    List<Ast.Comment> comments,
    List<AstToken> tokens
  ) {
    AstTree tree = new AstTree(factory, root, comments, tokens);
    tree.initialize();
    return tree;
  }

  protected void initialize() {
    root.forEach(e -> e.setTree(this));
    comments.stream().forEach(c -> c.forEach(e -> e.setTree(this)));
  }

  public List<AstToken> tokens() {
    return tokens;
  }

  public void addTokens(int index, AstNode node) {
    tokens.addAll(index, node.tree().tokens());
    node.forEach(t -> t.setTree(this));
  }

  public void addTokensAtLine(int line, AstNode node) {
    int lineStart = 0;
    if (line >= whitespace.lineCount()) {
      // add trailing newline if it doesn't exist.
      if (!(tokens().get(tokens().size() - 1) instanceof AstNewline)) {
        tokens().add(factory.createNewline());
      }

      lineStart = tokens().size();
    } else {
      lineStart = whitespace.lineTokenRange(line).start;
    }

    node.tokens().add(factory.createNewline());
    addTokens(lineStart, node);
  }

  public Optional<AstSyntaxError> getSyntaxError() {
    return this.syntaxError;
  }

  public void removeComment(AstNode comment) {
    comments.remove(comment);
    Range commentTokenRange = comment.tokenRange().get();
    Range removalRange = whitespace.includeLineWhitespaceOnOneSide(commentTokenRange);
    tokens.removeAll(tokens.subList(removalRange.start, removalRange.stop));
  }

  public void removeHiddenTokens() {
    // Ast's might have some non-whitespace tokens that are not attached to any tree object.
    // These happen when we have error nodes and possibly other reasons to remove things from ASTs
    // (e.g. current handling of preprocessor directives in C#)
    tokens.removeAll(
      tokens
        .stream()
        .filter(token -> token.parent.isEmpty() && !whitespace.isWhitespace(token))
        .collect(Collectors.toList())
    );
  }

  public void setSyntaxError(Optional<AstSyntaxError> error) {
    this.syntaxError = error;
  }

  public void updateComments() {
    comments.clear();
    Optional<Ast.Comment> previous = Optional.empty();
    for (AstToken token : tokens()) {
      Optional<Ast.Comment> current = token.ancestor(Ast.Comment.class);
      if (current.isPresent() && !current.equals(previous)) {
        comments.add(current.get());
        current.get().forEach(c -> c.setTree(this));
        previous = current;
      }
    }
  }
}
