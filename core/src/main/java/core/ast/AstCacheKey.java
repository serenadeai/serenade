package core.ast;

import core.gen.rpc.Language;
import java.util.Objects;

public class AstCacheKey {

  public final String source;
  public final Language language;

  public AstCacheKey(String source, Language language) {
    this.source = source;
    this.language = language;
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, language);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AstCacheKey) {
      AstCacheKey other = (AstCacheKey) obj;
      return Objects.equals(source, other.source) && Objects.equals(language, other.language);
    } else {
      return false;
    }
  }
}
