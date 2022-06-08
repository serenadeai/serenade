package core.selector;

import core.closeness.ClosestObjectFinder;
import core.util.Range;
import core.util.SearchDirection;
import core.util.selection.Selection;
import java.util.List;

public abstract class SelectionContext<T> {

  public final String source;
  public final Integer cursor;
  public final Selection selection;

  public abstract T closestLocation(ClosestObjectFinder closestObjectFinder, List<T> locations);

  public SearchDirection closestLocationDirection(T closestLocation) {
    Range closestRange = range(closestLocation);
    if (closestRange.stop < cursor) {
      return SearchDirection.PREVIOUS;
    } else if (closestRange.start > cursor) {
      return SearchDirection.NEXT;
    }
    return SearchDirection.NONE;
  }

  public abstract String nameString(T location);

  public abstract Range range(T location);

  public SelectionContext(String source, Integer cursor, Selection selection) {
    this.source = source;
    this.cursor = cursor;
    this.selection = selection;
  }
}
