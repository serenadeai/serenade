package core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ParseTree {

  private String type = "";
  private String name = "";
  private List<ParseTree> children = new ArrayList<>();
  private Optional<ParseTree> parent = Optional.empty();
  private String source = ""; // Note that this should be the same for every node in a tree.
  private int start = 0;
  private int stop = 0;

  public ParseTree(
    String type,
    String name,
    String source,
    int start,
    int stop,
    Optional<ParseTree> parent
  ) {
    this.type = type;
    this.name = name;
    this.source = source;
    this.start = start;
    this.stop = stop;
    this.parent = parent;
  }

  @Override
  public boolean equals(Object other) {
    if (other.getClass() != getClass()) {
      return false;
    }

    var o = (ParseTree) other;
    return (
      type.equals(o.type) &&
      name.equals(o.name) &&
      start == o.start &&
      stop == o.stop &&
      source.equals(o.source) &&
      children.equals(o.children)
    );
  }

  private String toDebugString(int level) {
    String result = "";
    String body = "";
    try {
      if (getStart() <= getStop()) {
        body = source.substring(getStart(), getStop());
      }
    } catch (Exception e) {
      body =
        "ERROR: Trying to substring(" + getStart() + "," + getStop() + ") from source: " + source;
    }

    for (int i = 0; i < level; i++) {
      result += "  ";
    }

    result += "<" + getType();
    if (!getName().equals("")) {
      result += " name=\"" + getName() + "\"";
    }

    result += ">";
    if (getChildren().size() > 0 || body.length() > 0) {
      result += "\n";
    }

    if (getChildren().size() > 0) {
      for (ParseTree child : getChildren()) {
        result += child.toDebugString(level + 1);
      }
    } else if (body.length() > 0) {
      for (int i = 0; i < level + 1; i++) {
        result += "  ";
      }

      result += body + "\n";
    }

    if (getChildren().size() > 0 || body.length() > 0) {
      for (int i = 0; i < level; i++) {
        result += "  ";
      }
    }

    result += "</" + getType() + ">\n";
    return result;
  }

  public List<ParseTree> getChildren() {
    return children;
  }

  public List<ParseTree> getChildren(String type) {
    return children.stream().filter(n -> n.type.equals(type)).collect(Collectors.toList());
  }

  public Optional<ParseTree> getChild(String type) {
    return children.stream().filter(n -> n.type.equals(type)).findFirst();
  }

  public String getSource() {
    return source;
  }

  public boolean isTerminal() {
    return this.children.size() == 0;
  }

  public Optional<ParseTree> getTerminal(String text) {
    return this.children.stream()
      .filter(n -> n.isTerminal() && n.getCode().equals(text))
      .findFirst();
  }

  public String getCode() {
    return source.substring(start, stop);
  }

  public int getIndex() {
    Optional<ParseTree> parent = getParent();
    if (parent.isEmpty()) {
      return 0;
    }

    for (int i = 0; i < parent.get().getChildren().size(); i++) {
      if (parent.get().getChildren().get(i) == this) {
        return i;
      }
    }

    return 0;
  }

  public String getName() {
    return name;
  }

  public Optional<ParseTree> getParent() {
    return parent;
  }

  public int getStart() {
    return start;
  }

  public int getStop() {
    return stop;
  }

  public String getType() {
    return type;
  }

  public void setChildren(List<ParseTree> children) {
    this.children = children;
  }

  public void setParent(Optional<ParseTree> parent) {
    this.parent = parent;
  }

  public String toDebugString() {
    return toDebugString(0);
  }

  public List<String> toMarkup() {
    List<String> tags = new ArrayList<>();
    if (this.isTerminal() && !this.getCode().equals("")) {
      tags.add(this.getCode());
    } else {
      tags.add("<" + this.type + ">");
      tags.addAll(
        children.stream().flatMap(node -> node.toMarkup().stream()).collect(Collectors.toList())
      );
      tags.add("</" + this.type + ">");
    }
    return tags;
  }
}
