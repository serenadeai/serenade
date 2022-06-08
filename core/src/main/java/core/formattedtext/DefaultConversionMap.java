package core.formattedtext;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultConversionMap extends ConversionMap {

  @Inject
  public DefaultConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_DEFAULT;
  }
}
