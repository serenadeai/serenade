package core.formattedtext;

import core.codeengine.Resolver;
import core.gen.rpc.Language;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JavaScriptConversionMap extends ConversionMap {

  @Inject
  public JavaScriptConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_JAVASCRIPT;
  }

  @Override
  protected void register() {
    super.register();

    registerLambda(e -> "(" + e + Resolver.wrapInSlot(Resolver.cursorOverride) + ") => {\n}");
    registerSymbol("right arrow", "=>");
    registerSymbol("arrow", "=>");
    registerSymbol("typeof", "typeof ");
    registerSymbol(Arrays.asList("instance of", "instanceof"), " instanceof ");

    // temporary fix to make lambda expressions work mostly as expected, until we remove
    // lambda from the disallow list in the code engine
    registerSymbol("const", "const ");
    registerSymbol("let", "let ");
    registerSymbol("var", "var ");
  }
}
