package core.commands;

import core.ast.Ast;
import core.ast.CPlusPlusAst;
import core.ast.api.AstListWithLinePadding;
import core.ast.api.AstNode;
import core.exception.ObjectNotFound;
import core.formattedtext.ConversionMapFactory;
import core.gen.rpc.Language;
import core.selector.Selector;
import core.snippet.SnippetCollectionFactory;
import core.util.Diff;
import core.util.selection.Selection;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CPlusPlusCommands extends Commands {

  @Inject
  public CPlusPlusCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_CPLUSPLUS;
  }

}
