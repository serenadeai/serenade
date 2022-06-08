package core.selector;

import core.closeness.ClosestObjectFinder;
import core.util.Range;
import core.util.selection.Selection;
import java.util.List;

public class RawSelectionContext extends SelectionContext<Range> {

  public RawSelectionContext(String source, Integer cursor, Selection selection) {
    super(source, cursor, selection);
  }

  public Range closestLocation(ClosestObjectFinder closestObjectFinder, List<Range> locations) {
    return closestObjectFinder.closestRange(source, cursor, locations);
  }

  public String nameString(Range location) {
    return source.substring(location.start, location.stop);
  }

  public Range range(Range location) {
    return location;
  }
}
