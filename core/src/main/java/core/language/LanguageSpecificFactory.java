package core.language;

import core.exception.LanguageFeatureNotSupported;
import core.gen.rpc.Language;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class LanguageSpecificFactory<T extends LanguageSpecific> {

  protected abstract List<T> elements();

  protected Optional<T> defaultValue() {
    return Optional.empty();
  }

  public T create(Language language) {
    for (T e : elements()) {
      if (e.language() == language) {
        return e;
      }
    }

    if (defaultValue().isPresent()) {
      return defaultValue().get();
    }

    throw new LanguageFeatureNotSupported();
  }
}
