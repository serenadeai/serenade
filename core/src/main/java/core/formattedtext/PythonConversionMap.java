package core.formattedtext;

import core.codeengine.Resolver;
import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PythonConversionMap extends ConversionMap {

  @Inject
  public PythonConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_PYTHON;
  }

  @Override
  protected void register() {
    super.register();

    registerSymbol("and", " and ");
    registerSymbol("or", " or ");
    registerSymbol("not", "not ");
    registerSymbol("is", " is ");
    registerSymbol("del attr", "delattr");
    registerSymbol("get attr", "getattr");
    registerSymbol("has attr", "hasattr");
    registerSymbol("set attr", "setattr");
    registerSymbol("non local", "nonlocal");
    registerSymbol("is instance", "isinstance");
    registerSymbol("is subclass", "issubclass");
    registerSymbol("class method", "classmethod");
    registerSymbol("static method", "staticmethod");
    registerSymbol("frozen set", "frozenset");
    registerSymbol("true", "True");
    registerSymbol("false", "False");
    registerSymbol("none", "None");
    registerSymbol("exponent", "**");
    registerLambda(e -> "lambda " + e + Resolver.wrapInSlot(Resolver.cursorOverride) + ": " + e);
  }

  @Override
  public String commentPrefix() {
    return "# ";
  }

  @Override
  public int indentation() {
    return 4;
  }

  @Override
  public String statementTerminator() {
    return "";
  }
}
