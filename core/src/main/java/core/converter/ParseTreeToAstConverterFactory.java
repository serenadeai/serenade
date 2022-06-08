package core.converter;

import core.ast.AstFactory;
import core.gen.rpc.Language;
import core.language.LanguageSpecificFactory;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParseTreeToAstConverterFactory
  extends LanguageSpecificFactory<ParseTreeToAstConverter> {

  @Inject
  BashParseTreeToAstConverter bashParseTreeToAstConverter;

  @Inject
  CPlusPlusParseTreeToAstConverter cPlusPlusParseTreeToAstConverter;

  @Inject
  CSharpParseTreeToAstConverter cSharpParseTreeToAstConverter;

  @Inject
  DartParseTreeToAstConverter dartParseTreeToAstConverter;

  @Inject
  DefaultParseTreeToAstConverter defaultParseTreeToAstConverter;

  @Inject
  GoParseTreeToAstConverter goParseTreeToAstConverter;

  @Inject
  HtmlParseTreeToAstConverter htmlParseTreeToAstConverter;

  @Inject
  JavaParseTreeToAstConverter javaParseTreeToAstConverter;

  @Inject
  JavaScriptParseTreeToAstConverter javaScriptParseTreeToAstConverter;

  @Inject
  KotlinParseTreeToAstConverter kotlinParseTreeToAstConverter;

  @Inject
  PythonParseTreeToAstConverter pythonParseTreeToAstConverter;

  @Inject
  RubyParseTreeToAstConverter rubyParseTreeToAstConverter;

  @Inject
  RustParseTreeToAstConverter rustParseTreeToAstConverter;

  @Inject
  ScssParseTreeToAstConverter scssParseTreeToAstConverter;

  @Inject
  public ParseTreeToAstConverterFactory() {}

  @Override
  protected List<ParseTreeToAstConverter> elements() {
    return Arrays.asList(
      bashParseTreeToAstConverter,
      cPlusPlusParseTreeToAstConverter,
      cSharpParseTreeToAstConverter,
      dartParseTreeToAstConverter,
      defaultParseTreeToAstConverter,
      goParseTreeToAstConverter,
      htmlParseTreeToAstConverter,
      javaParseTreeToAstConverter,
      javaScriptParseTreeToAstConverter,
      kotlinParseTreeToAstConverter,
      pythonParseTreeToAstConverter,
      rubyParseTreeToAstConverter,
      rustParseTreeToAstConverter,
      scssParseTreeToAstConverter
    );
  }
}
