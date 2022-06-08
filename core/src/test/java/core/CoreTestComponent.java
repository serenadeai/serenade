package core;

import core.ast.AstFactory;
import core.codeengine.CodeEngineClient;
import core.codeengine.Tokenizer;
import core.evaluator.TranscriptNormalizer;
import core.evaluator.TranscriptParser;
import core.parser.CommandAntlrParser;
import core.parser.Grammar;
import core.parser.GrammarAntlrParser;
import core.parser.Parser;
import core.visitor.TreeConverter;
import dagger.Component;
import javax.inject.Singleton;
import toolbelt.languages.LanguageDeterminer;
import toolbelt.state.History;
import toolbelt.state.UserState;

@Component(modules = { CoreTestModule.class, CoreSharedModule.class })
@Singleton
public interface CoreTestComponent {
  public AstFactory astFactory();

  public TreeConverter treeConverter();

  public History history();

  public Parser parser();

  public Grammar grammar();

  public GrammarAntlrParser grammarAntlrParser();

  public CommandAntlrParser commandAntlrParser();

  public CodeEngineClient codeEngineClient();

  public TranscriptParser transcriptParser();

  public TranscriptNormalizer transcriptNormalizer();

  public LanguageDeterminer languageDeterminer();

  public Tokenizer tokenizer();

  public UserState userState();
}
