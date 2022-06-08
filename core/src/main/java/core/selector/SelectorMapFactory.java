package core.selector;

import core.language.LanguageSpecificFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SelectorMapFactory extends LanguageSpecificFactory<SelectorMap> {

  @Inject
  BashSelectorMap bashSelectorMap;

  @Inject
  CPlusPlusSelectorMap cPlusPlusSelectorMap;

  @Inject
  CSharpSelectorMap cSharpSelectorMap;

  @Inject
  DartSelectorMap dartSelectorMap;

  @Inject
  DefaultSelectorMap defaultSelectorMap;

  @Inject
  GoSelectorMap goSelectorMap;

  @Inject
  HtmlSelectorMap htmlSelectorMap;

  @Inject
  JavaSelectorMap javaSelectorMap;

  @Inject
  JavaScriptSelectorMap javaScriptSelectorMap;

  @Inject
  KotlinSelectorMap kotlinSelectorMap;

  @Inject
  PythonSelectorMap pythonSelectorMap;

  @Inject
  RubySelectorMap rubySelectorMap;

  @Inject
  RustSelectorMap rustSelectorMap;

  @Inject
  ScssSelectorMap scssSelectorMap;

  @Inject
  public SelectorMapFactory() {}

  @Override
  public Optional<SelectorMap> defaultValue() {
    return Optional.of(defaultSelectorMap);
  }

  @Override
  protected List<SelectorMap> elements() {
    return Arrays.asList(
      bashSelectorMap,
      cPlusPlusSelectorMap,
      cSharpSelectorMap,
      dartSelectorMap,
      defaultSelectorMap,
      goSelectorMap,
      htmlSelectorMap,
      javaSelectorMap,
      javaScriptSelectorMap,
      kotlinSelectorMap,
      pythonSelectorMap,
      rubySelectorMap,
      rustSelectorMap,
      scssSelectorMap
    );
  }
}
