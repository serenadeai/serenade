package core.snippet;

import core.language.LanguageSpecificFactory;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SnippetCollectionFactory extends LanguageSpecificFactory<SnippetCollection> {

  @Inject
  BashSnippetCollection bashSnippetCollection;

  @Inject
  CPlusPlusSnippetCollection cPlusPlusSnippetCollection;

  @Inject
  CSharpSnippetCollection cSharpSnippetCollection;

  @Inject
  DartSnippetCollection dartSnippetCollection;

  @Inject
  DefaultSnippetCollection defaultSnippetCollection;

  @Inject
  GoSnippetCollection goSnippetCollection;

  @Inject
  HtmlSnippetCollection htmlSnippetCollection;

  @Inject
  JavaSnippetCollection javaSnippetCollection;

  @Inject
  JavaScriptSnippetCollection javaScriptSnippetCollection;

  @Inject
  KotlinSnippetCollection kotlinSnippetCollection;

  @Inject
  PythonSnippetCollection pythonSnippetCollection;

  @Inject
  RubySnippetCollection rubySnippetCollection;

  @Inject
  RustSnippetCollection rustSnippetCollection;

  @Inject
  ScssSnippetCollection scssSnippetCollection;

  @Inject
  public SnippetCollectionFactory() {}

  @Override
  protected List<SnippetCollection> elements() {
    return Arrays.asList(
      bashSnippetCollection,
      cPlusPlusSnippetCollection,
      cSharpSnippetCollection,
      dartSnippetCollection,
      defaultSnippetCollection,
      goSnippetCollection,
      htmlSnippetCollection,
      javaSnippetCollection,
      javaScriptSnippetCollection,
      kotlinSnippetCollection,
      pythonSnippetCollection,
      rubySnippetCollection,
      rustSnippetCollection,
      scssSnippetCollection
    );
  }
}
