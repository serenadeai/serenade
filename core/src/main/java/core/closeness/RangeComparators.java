package core.closeness;

import core.util.LinePositionConverter;
import core.util.Range;
import java.util.Comparator;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RangeComparators {

  @Inject
  public RangeComparators() {}

  public Comparator<Range> distanceToEndpoint(int cursor) {
    return Comparator.comparingInt(
      r -> Math.min(Math.abs(cursor - r.stop), Math.abs(r.start - cursor))
    );
  }

  public Comparator<Range> distanceToInside(int cursor) {
    return Comparator.comparingInt(r -> Math.max(cursor - r.stop, Math.max(r.start - cursor, 0)));
  }

  public Comparator<Range> nested() {
    return (r1, r2) -> {
      // r1 inside r2
      if (r1.start >= r2.start && r1.stop <= r2.stop) {
        return -1;
      } else if (r2.start >= r1.start && r2.stop <= r1.stop) { // r2 inside r1
        return 1;
      } else {
        return 0;
      }
    };
  }

  public Comparator<Range> lineBorderDistanceToInside(String source, int cursor) {
    LinePositionConverter linePositionConverter = new LinePositionConverter(source);

    // Pick the border between this line and the next as the position.
    int linePosition = linePositionConverter.position(cursor) + 1;
    return Comparator.comparing(
      e -> linePositionConverter.range(e),
      distanceToInside(linePosition)
    );
  }

  public Comparator<Range> lineDistanceToEndpoint(String source, int cursor) {
    LinePositionConverter linePositionConverter = new LinePositionConverter(source);
    int linePosition = linePositionConverter.position(cursor);

    // When positions are lines, instead of the spaces between lines, the range
    // should be inclusive.
    return Comparator.comparing(
      e -> {
        Range r = linePositionConverter.range(e);
        return new Range(r.start, r.stop - 1);
      },
      distanceToEndpoint(linePosition)
    );
  }

  public Comparator<Range> prioritizeRight(String source, int cursor) {
    return Comparator.comparing(r -> r.start >= cursor ? 0 : 1);
  }
}
