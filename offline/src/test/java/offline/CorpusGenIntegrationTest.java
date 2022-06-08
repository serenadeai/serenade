package offline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import core.ast.api.AstNode;
import core.gen.rpc.Language;
import corpusgen.mapping.Config;
import corpusgen.mapping.FileFilter;
import corpusgen.mapping.FullContextMapping;
import corpusgen.mapping.FullContextMappingGenerator;
import corpusgen.mapping.Snippets;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import offline.subcommands.SmallRepositories;
import org.junit.jupiter.api.Test;

public class CorpusGenIntegrationTest {

  private final int fileCount = 250;
  private OfflineComponent component = DaggerOfflineComponent.builder().build();

  @SuppressWarnings("unchecked")
  protected void checkLanguage(Language language) {
    Optional<Set<Class<? extends AstNode>>> include = Optional.empty();
    if (System.getenv("CORPUSGEN_INTEGRATION_TEST_INCLUDE") != null) {
      include =
        Optional.of(
          Arrays
            .asList(System.getenv("CORPUSGEN_INTEGRATION_TEST_INCLUDE").split(","))
            .stream()
            .map(
              class_ -> {
                try {
                  return (Class<? extends AstNode>) Class.forName(
                    "core.ast." + class_.replace('.', '$')
                  );
                } catch (ClassNotFoundException e) {
                  throw new RuntimeException(e);
                }
              }
            )
            .collect(Collectors.toSet())
        );
    }

    Config config = new Config();
    config.language = language;
    config.sampleAlternativeWords = false;
    config.includeAddMappings = true;
    FullContextMappingGenerator fullContextMappingGenerator = component
      .fullContextMappingGeneratorFactory()
      .create(config);
    List<Path> parsablePaths = component.smallRepositories().pathsReturningAst(language);

    assert parsablePaths.size() > fileCount : "Not enough files checked for " + language + ": " + parsablePaths.size();
    parsablePaths = parsablePaths.subList(0, fileCount);

    int mappingCount = 0;
    for (Path path : parsablePaths) {
      String source;
      try {
        source = new String(Files.readAllBytes(path));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      List<FullContextMapping> mappings = new ArrayList<>();
      try {
        mappings = fullContextMappingGenerator.generateMappings(source, include);
        for (FullContextMapping m : mappings) {
          if (mappingCount % 25 == 0) {
            m.sampleInput();
          }
          mappingCount++;
        }
      } catch (Exception e) {
        System.out.println("Failure for file: " + path.toString());
        //throw e;
      }
    }
  }

  @Test
  public void testCPlusPlus() {
    checkLanguage(Language.LANGUAGE_CPLUSPLUS);
  }

  @Test
  public void testCSharp() {
    checkLanguage(Language.LANGUAGE_CSHARP);
  }

  @Test
  public void testGo() {
    checkLanguage(Language.LANGUAGE_GO);
  }

  @Test
  public void testHtml() {
    checkLanguage(Language.LANGUAGE_HTML);
  }

  @Test
  public void testJava() {
    checkLanguage(Language.LANGUAGE_JAVA);
  }

  @Test
  public void testJavaScript() {
    checkLanguage(Language.LANGUAGE_JAVASCRIPT);
  }

  @Test
  public void testPython() {
    checkLanguage(Language.LANGUAGE_PYTHON);
  }

  @Test
  public void testRuby() {
    checkLanguage(Language.LANGUAGE_RUBY);
  }

  @Test
  public void testRust() {
    checkLanguage(Language.LANGUAGE_RUST);
  }

  @Test
  public void testScss() {
    checkLanguage(Language.LANGUAGE_SCSS);
  }
}
