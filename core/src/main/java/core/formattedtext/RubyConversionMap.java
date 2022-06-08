package core.formattedtext;

import core.codeengine.Resolver;
import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RubyConversionMap extends ConversionMap {

  @Inject
  public RubyConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_RUBY;
  }

  @Override
  protected void register() {
    super.register();
  }

  @Override
  public String commentPrefix() {
    return "# ";
  }

  @Override
  public int indentation() {
    return 3;
  }

  @Override
  public String statementTerminator() {
    return "";
  }
}
