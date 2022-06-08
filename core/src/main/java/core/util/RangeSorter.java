package core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RangeSorter {

  @Inject
  public RangeSorter() {}

  public List<Range> preorder(List<Range> ranges) {
    return preorder(ranges, r -> r);
  }

  public <T> List<T> preorder(Collection<T> locations, Function<T, Range> range) {
    // Sorts the ranges into the order they would appear in a preorder traversal of the implicit
    // underlying tree.
    ArrayList<T> sorted = new ArrayList<T>(locations);
    Collections.sort(
      sorted,
      Comparator
        .<T>comparingInt(location -> range.apply(location).start)
        .thenComparing(Comparator.<T>comparingInt(location -> -range.apply(location).stop))
    );
    return sorted;
  }
}
