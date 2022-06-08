package core.commands;

import core.ast.Ast;
import core.ast.JavaScriptAst;
import core.ast.api.AstNode;
import core.ast.api.AstParent;
import core.ast.api.AstToken;
import core.exception.LanguageFeatureNotSupported;
import core.exception.ObjectNotFound;
import core.formattedtext.ConversionMapFactory;
import core.gen.rpc.Language;
import core.selector.Selector;
import core.snippet.SnippetCollectionFactory;
import core.util.Diff;
import core.util.Range;
import core.util.selection.Selection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JavaScriptCommands extends Commands {

  @Inject
  public JavaScriptCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_JAVASCRIPT;
  }
}
