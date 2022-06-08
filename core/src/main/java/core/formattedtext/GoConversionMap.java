package core.formattedtext;

import core.codeengine.Resolver;
import core.gen.rpc.Language;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GoConversionMap extends ConversionMap {

  @Inject
  public GoConversionMap() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_GO;
  }

  @Override
  protected void register() {
    super.register();
  }

  @Override
  public String commentPrefix() {
    return "// ";
  }

  @Override
  public int indentation() {
    return 4;
  }

  @Override
  public List<List<String>> lambdaPrefixes() {
    List<List<String>> ret = new ArrayList<>(super.lambdaPrefixes());
    ret.add(Arrays.asList("func"));
    ret.add(Arrays.asList("func", "of"));
    return ret;
  }

  @Override
  public String statementTerminator() {
    return "";
  }
}
