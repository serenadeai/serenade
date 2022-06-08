package corpusgen;

import core.CoreModule;
import core.CoreSharedModule;
import core.codeengine.Resolver;
import core.codeengine.Tokenizer;
import core.evaluator.TranscriptParser;
import core.formattedtext.ConversionMapFactory;
import core.parser.Grammar;
import core.parser.GrammarAntlrParser;
import core.parser.GrammarParser;
import core.util.NumberConverter;
import corpusgen.command.Chainer;
import corpusgen.command.CommandGenerator;
import corpusgen.mapping.FileFilter;
import corpusgen.mapping.FullContextMappingGenerator;
import corpusgen.mapping.FullContextMappingGenerator;
import corpusgen.util.NumberGenerator;
import dagger.Component;
import javax.inject.Singleton;
import toolbelt.languages.LanguageDeterminer;

@Singleton
@Component(modules = { CoreModule.class, CoreSharedModule.class, CorpusGenModule.class })
public interface CorpusGenComponent {
  public Chainer chainer();

  public CommandGenerator commandGenerator();

  public ConversionMapFactory conversionMapFactory();

  public CorpusGen corpusGen();

  public FileFilter fileFilter();

  public FullContextMappingGenerator.Factory fullContextMappingGeneratorFactory();

  public Grammar grammar();

  public GrammarAntlrParser grammarAntlrParser();

  public LanguageDeterminer languageDeterminer();

  public NumberConverter numberConverter();

  public NumberGenerator numberGenerator();

  public GrammarParser parser();

  public Resolver resolver();

  public Tokenizer tokenizer();

  public TranscriptParser transcriptParser();
}
