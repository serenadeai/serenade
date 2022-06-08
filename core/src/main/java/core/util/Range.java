package core.util;

import com.google.common.base.MoreObjects;
import java.util.Objects;

public class Range {

  public int start;
  public int stop;

  public Range(Range range) {
    this.start = range.start;
    this.stop = range.stop;
  }

  public Range(int start, int stop) {
    this.start = start;
    this.stop = stop;
  }

  public Range(int index) {
    this(index, index + 1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.start, this.stop);
  }

  @Override
  public boolean equals(Object o) {
    if (this.getClass() != o.getClass()) {
      return false;
    }

    Range r = (Range) o;
    return this.start == r.start && this.stop == r.stop;
  }

  public boolean contains(int index) {
    return start <= index && index < stop;
  }

  public boolean contains(Range range) {
    return range.inside(this);
  }

  public boolean inside(Range range) {
    return range.start <= this.start && this.stop <= range.stop;
  }

  public boolean overlaps(Range range) {
    // the size of the intersection is greater than 0.
    return Math.min(this.stop, range.stop) - Math.max(this.start, range.start) > 0;
  }

  public int length() {
    return stop - start;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("start", start).add("stop", stop).toString();
  }
}
