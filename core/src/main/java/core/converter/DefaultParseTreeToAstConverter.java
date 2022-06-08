package core.converter;

import core.ast.api.AstNode;
import core.ast.api.DefaultAstParent;
import core.gen.rpc.Language;
import core.parser.ParseTree;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultParseTreeToAstConverter extends ParseTreeToAstConverter {

  @Inject
  public DefaultParseTreeToAstConverter() {
    initialize();
  }

  @Override
  public Language language() {
    return Language.LANGUAGE_DEFAULT;
  }

  @Override
  protected void registerConverters() {}

  @Override
  public AstNode convert(String source, ParseTree node) {
    return new DefaultAstParent();
  }
}
