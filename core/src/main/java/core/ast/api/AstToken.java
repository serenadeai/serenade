package core.ast.api;

import core.parser.ParseTree;
import core.util.Range;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AstToken implements AstNode, Cloneable {

  protected List<AstNode> children = new ArrayList<>();
  protected Optional<AstParent> parent = Optional.empty();
  protected AstTree tree;

  // Metadata used during tree creation.
  protected Optional<ParseTree> parseTree = Optional.empty();

  public String code;

  // maintained by AstTokens
  public Range range;
  public int priorNewlines;
  public int index;

  @Override
  public List<AstNode> children() {
    return Collections.<AstNode>emptyList();
  }

  @Override
  public AstToken cloneTree(Optional<AstParent> parent, Map<AstToken, AstToken> oldToNewTokens) {
    AstToken ret = oldToNewTokens.get(this);
    ret.setParent(parent);
    return ret;
  }

  public AstToken clone() throws CloneNotSupportedException {
    return (AstToken) super.clone();
  }

  @Override
  public boolean isToken(String code) {
    return this.code.equals(code);
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
  public Range range() {
    return range;
  }

  @Override
  public String code() {
    return this.code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public void setParseTree(Optional<ParseTree> parseTree) {
    this.parseTree = parseTree;
  }

  @Override
  public void setParent(Optional<AstParent> parent) {
    this.parent = parent;
  }

  @Override
  public void setTree(AstTree tree) {
    this.tree = tree;
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
    result += "<Token type=\"" + getClass().getSimpleName() + "\">";
    if (!code.equals("")) {
      result += "\n";
      for (int i = 0; i < level + 1; i++) {
        result += "  ";
      }
      result += code + "\n";
    }

    for (int i = 0; i < level; i++) {
      result += "  ";
    }
    result += "</Token>";

    return result;
  }
}
