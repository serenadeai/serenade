package core.selector;

import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.closeness.ClosestObjectFinder;
import core.util.Range;
import core.util.selection.Selection;
import java.util.List;

public class AstSelectionContext extends SelectionContext<AstNode> {

  public final AstParent root;

  public AstSelectionContext(String source, Integer cursor, Selection selection, AstParent root) {
    super(source, cursor, selection);
    this.root = root;
  }

  public AstNode closestLocation(ClosestObjectFinder closestObjectFinder, List<AstNode> locations) {
    return closestObjectFinder.closestNode(root, source, locations.stream(), cursor);
  }

  public String nameString(AstNode location) {
    return location.nameString().toLowerCase();
  }

  public Range range(AstNode location) {
    return location.range();
  }
}
