package core.formattedtext;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HtmlConversionMap extends ConversionMap {

  @Inject
  public HtmlConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_HTML;
  }

  @Override
  public String commentPrefix() {
    return "<!--";
  }

  @Override
  public String commentPostfix() {
    return "-->";
  }

  @Override
  public String statementTerminator() {
    return "";
  }
}
