package core.formattedtext;

import core.codeengine.Resolver;
import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JavaConversionMap extends ConversionMap {

  @Inject
  public JavaConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_JAVA;
  }

  @Override
  protected void register() {
    super.register();

    registerLambda(e -> "(" + e + Resolver.wrapInSlot(Resolver.cursorOverride) + ") -> {\n}");
    registerSymbol("empty list", "{}");
  }
}
