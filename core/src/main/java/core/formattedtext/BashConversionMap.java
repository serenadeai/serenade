package core.formattedtext;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BashConversionMap extends ConversionMap {

  @Inject
  public BashConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_BASH;
  }

  @Override
  public String commentPrefix() {
    return "# ";
  }

  @Override
  public String statementTerminator() {
    return "";
  }
}
