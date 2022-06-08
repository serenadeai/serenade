package core.formattedtext;

import core.language.LanguageSpecificFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConversionMapFactory extends LanguageSpecificFactory<ConversionMap> {

  @Inject
  BashConversionMap bashConversionMap;

  @Inject
  DefaultConversionMap defaultConversionMap;

  @Inject
  GoConversionMap goConversionMap;

  @Inject
  HtmlConversionMap htmlConversionMap;

  @Inject
  JavaConversionMap javaConversionMap;

  @Inject
  JavaScriptConversionMap javaScriptConversionMap;

  @Inject
  KotlinConversionMap kotlinConversionMap;

  @Inject
  PythonConversionMap pythonConversionMap;

  @Inject
  RubyConversionMap rubyConversionMap;

  @Inject
  public ConversionMapFactory() {}

  protected Optional<ConversionMap> defaultValue() {
    return Optional.of(defaultConversionMap);
  }

  protected List<ConversionMap> elements() {
    return Arrays.asList(
      bashConversionMap,
      defaultConversionMap,
      goConversionMap,
      htmlConversionMap,
      javaConversionMap,
      javaScriptConversionMap,
      kotlinConversionMap,
      pythonConversionMap,
      rubyConversionMap
    );
  }
}
