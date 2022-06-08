package core.util;

import com.google.common.base.MoreObjects;

public class Change {

  public final Range range;
  public final String substitution;

  public Change(Range range, String substitution) {
    this.range = range;
    this.substitution = substitution;
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("range", range)
      .add("substitution", substitution)
      .toString();
  }
}
