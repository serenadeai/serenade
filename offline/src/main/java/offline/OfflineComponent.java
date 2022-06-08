package offline;

import core.CoreModule;
import core.CoreSharedModule;
import core.ast.AstFactory;
import core.converter.ParseTreeToAstConverterFactory;
import corpusgen.CorpusGenModule;
import corpusgen.mapping.FileFilter;
import corpusgen.mapping.FullContextMappingGenerator;
import dagger.Component;
import grammarflattener.GrammarFlattener;
import javax.inject.Singleton;
import offline.subcommands.BenchmarkRunner;
import offline.subcommands.SmallRepositories;
import offline.subcommands.TreePrinter;
import offline.subcommands.TutorialGenerator;
import toolbelt.languages.LanguageDeterminer;

@Singleton
@Component(
  modules = { CorpusGenModule.class, CoreModule.class, CoreSharedModule.class, OfflineModule.class }
)
public interface OfflineComponent {
  public AstFactory astFactory();

  public BenchmarkRunner benchmarkRunner();

  public FileFilter fileFilter();

  public GrammarFlattener flattener();

  public FullContextMappingGenerator.Factory fullContextMappingGeneratorFactory();

  public LanguageDeterminer languageDeterminer();

  public ParseTreeToAstConverterFactory parseTreeToAstConverterFactory();

  public SmallRepositories smallRepositories();

  public TreePrinter treePrinter();

  public TutorialGenerator tutorialGenerator();
}
