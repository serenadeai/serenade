package core.util.selection;

import com.google.common.base.MoreObjects;
import core.util.ObjectType;
import core.util.Range;
import core.util.SearchDirection;
import java.util.Optional;

public class Selection {

  public static class Builder {

    private ObjectType object;

    private SelectionEndpoint endpoint = SelectionEndpoint.NONE;
    private Optional<Range> absoluteRange = Optional.empty();
    private Optional<String> name = Optional.empty();
    private boolean fromCursorToObject = false;
    private SearchDirection direction = SearchDirection.NONE;
    private Optional<Integer> count = Optional.empty(); // How many of the object to select
    private Optional<Integer> offset = Optional.empty(); // How many of the object to skip over
    private Optional<String> transcript = Optional.empty();

    public Builder(ObjectType object) {
      this.object = object;
    }

    public Builder(Selection selection) {
      this.object = selection.object;
      this.endpoint = selection.endpoint;
      this.absoluteRange = selection.absoluteRange;
      this.name = selection.name;
      this.fromCursorToObject = selection.fromCursorToObject;
      this.direction = selection.direction;
      this.count = selection.count;
      this.offset = selection.offset;
      this.transcript = transcript;
    }

    public Selection build() {
      return new Selection(
        object,
        endpoint,
        absoluteRange,
        name,
        fromCursorToObject,
        direction,
        count,
        offset,
        transcript
      );
    }

    public Builder setEndpoint(SelectionEndpoint endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder setAbsoluteRange(Optional<Range> absoluteRange) {
      this.absoluteRange = absoluteRange;
      return this;
    }

    public Builder setAbsoluteRange(Range absoluteRange) {
      this.absoluteRange = Optional.of(absoluteRange);
      return this;
    }

    public Builder setName(Optional<String> name) {
      this.name = name;
      return this;
    }

    public Builder setName(String name) {
      if (!name.equals("")) {
        setName(Optional.of(name));
      }

      return this;
    }

    public Builder setFromCursorToObject(boolean fromCursorToObject) {
      this.fromCursorToObject = fromCursorToObject;
      return this;
    }

    public Builder setDirection(SearchDirection direction) {
      this.direction = direction;
      return this;
    }

    public Builder setCount(Optional<Integer> count) {
      this.count = count;
      return this;
    }

    public Builder setCount(int count) {
      this.count = Optional.of(count);
      return this;
    }

    public Builder setOffset(int offset) {
      this.offset = Optional.of(offset);
      return this;
    }

    public Builder setTranscript(String transcript) {
      this.transcript = Optional.of(transcript);
      return this;
    }

    @Override
    public String toString() {
      return MoreObjects
        .toStringHelper(this)
        .add("object", object)
        .add("endpoint", endpoint)
        .add("name", name)
        .add("absoluteRange", absoluteRange)
        .add("fromCursorToObject", fromCursorToObject)
        .add("direction", direction)
        .add("count", count)
        .add("offset", offset)
        .add("transcript", transcript)
        .toString();
    }
  }

  public final ObjectType object;
  public final SelectionEndpoint endpoint;
  public final Optional<String> name;

  // indexes over selection objects, not character positions
  public final Optional<Range> absoluteRange;

  // currently just used for the phrase selectors.
  public final boolean fromCursorToObject;
  public final SearchDirection direction;

  // indexes over selection objects, not character positions.
  public final Optional<Integer> count;
  public final Optional<Integer> offset;
  public final Optional<String> transcript;

  private Selection(
    ObjectType object,
    SelectionEndpoint endpoint,
    Optional<Range> absoluteRange,
    Optional<String> name,
    boolean fromCursorToObject,
    SearchDirection direction,
    Optional<Integer> count,
    Optional<Integer> offset,
    Optional<String> transcript
  ) {
    this.object = object;
    this.endpoint = endpoint;
    this.absoluteRange = absoluteRange;
    this.name = name;
    this.fromCursorToObject = fromCursorToObject;
    this.direction = direction;
    this.count = count;
    this.offset = offset;
    this.transcript = transcript;
  }

  @Override
  public boolean equals(Object o) {
    if (this.getClass() != o.getClass()) {
      return false;
    }

    Selection that = (Selection) o;
    return (
      this.object == that.object &&
      this.absoluteRange.equals(that.absoluteRange) &&
      this.endpoint == that.endpoint &&
      this.name.equals(that.name) &&
      this.fromCursorToObject == that.fromCursorToObject &&
      this.direction == that.direction &&
      this.count.equals(that.count) &&
      this.offset.equals(that.offset)
    );
  }

  public boolean hasNonTextModifier() {
    return !(new Builder(this.object).setName(this.name).build()).equals(this);
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("object", object)
      .add("endpoint", endpoint)
      .add("name", name)
      .add("absoluteRange", absoluteRange)
      .add("fromCursorToObject", fromCursorToObject)
      .add("direction", direction)
      .add("count", count)
      .add("offset", offset)
      .add("transcript", transcript)
      .toString();
  }
}
