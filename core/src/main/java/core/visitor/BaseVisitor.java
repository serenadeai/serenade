package core.visitor;

import core.parser.ParseTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.inject.Inject;

public abstract class BaseVisitor<ContextType, ReturnType> {

  private Map<String, BiFunction<ParseTree, ContextType, ReturnType>> typeToVisitor = new HashMap<>();

  protected ReturnType aggregateResult(ReturnType aggregate, ReturnType next) {
    return next;
  }

  protected ReturnType defaultResult() {
    return null;
  }

  protected void register(String type, BiFunction<ParseTree, ContextType, ReturnType> visitor) {
    typeToVisitor.put(type, visitor);
  }

  public ReturnType visit(ParseTree node, ContextType context) {
    if (typeToVisitor.containsKey(node.getType())) {
      return typeToVisitor.get(node.getType()).apply(node, context);
    } else if (node.getChildren().size() > 0) {
      return this.visitChildren(node, context);
    } else {
      return this.visitTerminal(node, context);
    }
  }

  public ReturnType visitChildren(ParseTree node, ContextType context) {
    ReturnType result = this.defaultResult();
    ReturnType visitResult = null;
    for (ParseTree child : node.getChildren()) {
      visitResult = visit(child, context);
      if (visitResult != null) {
        result = this.aggregateResult(result, visitResult);
      }
    }

    return result;
  }

  public ReturnType visitTerminal(ParseTree node, ContextType context) {
    return this.defaultResult();
  }
}
