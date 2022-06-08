package core.formattedtext;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KotlinConversionMap extends ConversionMap {

  @Inject
  public KotlinConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_KOTLIN;
  }

  @Override
  public String statementTerminator() {
    return "";
  }
}
