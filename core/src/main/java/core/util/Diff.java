package core.util;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class Diff {

  private final String initialSource;
  private final List<Change> changes;
  private final int cursor;

  private Diff(String initialSource, int cursor, List<Change> changes) {
    this.initialSource = initialSource;
    this.changes = changes;
    this.cursor = cursor;
  }

  @Override
  public boolean equals(Object o) {
    if (getClass() != o.getClass()) {
      return false;
    }

    Diff d = (Diff) o;
    return getSource().equals(d.getSource()) && getCursor() == d.getCursor();
  }

  public static Diff fromInitialState(String initialSource, int cursor) {
    return new Diff(initialSource, cursor, Collections.emptyList());
  }

  public List<Change> getChanges() {
    return changes;
  }

  public int getCursor() {
    return cursor;
  }

  public String getSource() {
    StringBuilder builder = new StringBuilder(initialSource);
    // Couldn't see anything in the docs about which indexing replace is using,
    // so just doing this backwards just in case it's not the indexing of the original.
    for (int i = changes.size() - 1; i >= 0; i--) {
      Change change = changes.get(i);
      builder.replace(change.range.start, change.range.stop, change.substitution);
    }
    return builder.toString();
  }

  public Diff insert(int index, String string) {
    return replaceRange(new Range(index, index), string);
  }

  public Diff insertStringAndMoveCursorToStop(int index, String string) {
    return replaceRangeAndMoveCursorToStop(new Range(index, index), string);
  }

  public Diff moveCursor(int cursor) {
    return new Diff(initialSource, cursor, changes);
  }

  public Diff then(Diff remaining) {
    // allows us to apply changes to the final source and cursor as if it's the initial one.
    // Changes get applied in backwards order, so put ours at the end. Any
    List<Change> changes = new ArrayList<>(remaining.changes);
    changes.addAll(this.changes);
    return new Diff(initialSource, remaining.cursor, changes);
  }

  public Diff replaceRange(Range range, String substitution) {
    List<Change> changes = new ArrayList<>(this.changes);
    int newCursor = cursor;
    if (!(substitution.equals("") && range.length() == 0)) { // strip no-ops from change lists.
      changes.add(new Change(range, substitution));
      // while it's unclear if ordering matters for the server, it matters for the client.
      Collections.sort(changes, Comparator.comparingInt(c -> c.range.start));
      if (newCursor > range.start + substitution.length()) {
        newCursor += substitution.length() - range.length();
      }
    }
    return new Diff(initialSource, newCursor, changes);
  }

  public Diff replaceRangeAndMoveCursorToStop(Range range, String string) {
    return replaceRange(range, string).moveCursor(range.start + string.length());
  }

  public Diff replaceSource(String newSource) {
    return replaceRange(new Range(0, initialSource.length()), newSource);
  }

  public Diff withoutChanges() {
    // to be used in conjunction with "then" in order to apply changes to the final state.
    return Diff.fromInitialState(getSource(), getCursor());
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("source", getSource())
      .add("cursor", getCursor())
      .toString();
  }
}
