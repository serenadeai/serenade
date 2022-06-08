package core.snippet;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.Set;

public class SnippetCacheKey {

  public final int cursor;
  public final String source;
  public final Snippet.Transform transform;

  public SnippetCacheKey(String source, int cursor, Snippet.Transform transform) {
    this.source = source;
    this.cursor = cursor;
    this.transform = transform;
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("source", source)
      .add("transform", transform)
      .add("cursor", cursor)
      .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, cursor, transform);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SnippetCacheKey) {
      SnippetCacheKey other = (SnippetCacheKey) obj;
      return (
        Objects.equals(cursor, other.cursor) &&
        Objects.equals(transform, other.transform) &&
        Objects.equals(source, other.source)
      );
    }

    return false;
  }
}
