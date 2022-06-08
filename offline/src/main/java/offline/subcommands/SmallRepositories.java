package offline.subcommands;

import core.gen.rpc.Language;
import corpusgen.mapping.FileFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import offline.Subcommand;
import toolbelt.env.Env;
import toolbelt.languages.LanguageDeterminer;

@Singleton
public class SmallRepositories {

  @Inject
  Env env;

  @Inject
  FileFilter fileFilter;

  @Inject
  LanguageDeterminer languageDeterminer;

  @Inject
  public SmallRepositories() {}

  public List<Path> paths(Language language) {
    String name = languageDeterminer.enumString(language);
    if (name.equals("")) {
      return Collections.emptyList();
    } else if (name.equals("javascript")) {
      name = "typescript";
    }
    try (
      Stream<Path> paths = Files.walk(
        Paths.get(env.libraryRoot() + "/repositories/" + name + "-small")
      )
    ) {
      return paths.filter(Files::isRegularFile).collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<Path> pathsReturningAst(Language language) {
    return paths(language)
      .stream()
      .filter(path -> fileFilter.processableFile(path, language))
      .collect(Collectors.toList());
  }
}
