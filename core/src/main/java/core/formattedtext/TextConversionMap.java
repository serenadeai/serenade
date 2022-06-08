package core.formattedtext;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TextConversionMap extends ConversionMap {

  @Inject
  public TextConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_NONE;
  }

  @Override
  protected void register() {
    super.register();

    deregisterSymbol("and");
    deregisterSymbol("or");
    deregisterSymbol("binary or");
    deregisterSymbol("binary xor");
    deregisterSymbol("binary complement");
    deregisterSymbol("bang");
    deregisterSymbol("exclam");
    deregisterSymbol("not");
    deregisterSymbol("mod");
    deregisterSymbol("diamond");
    deregisterSymbol("semi");
    deregisterSymbol("times");

    deregisterEnclosure("of");
    deregisterEnclosure("call");
    deregisterEnclosureAndSymbols("sub");

    deregisterRecursive("open tag");
    deregisterRecursive("close tag");
    deregisterRecursive("empty tag");
    deregisterRecursive("tag");
  }
}
