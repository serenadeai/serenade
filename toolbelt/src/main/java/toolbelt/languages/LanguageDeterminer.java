package toolbelt.languages;

import core.gen.rpc.Language;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LanguageDeterminer {

  private class Configuration {

    public List<String> apiNames;
    public List<String> extensions;
    public boolean mlSnippetsDisabled = false;

    public Configuration(List<String> apiNames, List<String> extensions) {
      this(apiNames, extensions, false);
    }

    public Configuration(
      List<String> apiNames,
      List<String> extensions,
      boolean mlSnippetsDisabled
    ) {
      this.apiNames = apiNames;
      this.extensions = extensions;
      this.mlSnippetsDisabled = mlSnippetsDisabled;
    }
  }

  private Map<Language, Configuration> configurations = new HashMap<>(
    Map.ofEntries(
      Map.entry(
        Language.LANGUAGE_BASH,
        new Configuration(Arrays.asList("bash"), Arrays.asList("bash", "sh"), true)
      ),
      Map.entry(
        Language.LANGUAGE_CPLUSPLUS,
        new Configuration(
          Arrays.asList("cpp", "c++", "c", "c plus plus", "cplusplus"),
          Arrays.asList("c", "cc", "cpp", "cxx", "c++", "h", "hh", "hpp", "hxx", "h++")
        )
      ),
      Map.entry(
        Language.LANGUAGE_CSHARP,
        new Configuration(Arrays.asList("csharp", "c#", "c sharp"), Arrays.asList("cs"))
      ),
      Map.entry(
        Language.LANGUAGE_DART,
        new Configuration(Arrays.asList("dart"), Arrays.asList("dart"), true)
      ),
      Map.entry(
        Language.LANGUAGE_DEFAULT,
        new Configuration(
          Arrays.asList("english", "text", "default"),
          Arrays.asList("json", "md", "rst", "toml", "txt", "yaml", "yml")
        )
      ),
      Map.entry(
        Language.LANGUAGE_GO,
        new Configuration(Arrays.asList("go", "golang"), Arrays.asList("go"))
      ),
      Map.entry(
        Language.LANGUAGE_HTML,
        new Configuration(
          Arrays.asList("html", "svelte", "vue", "xaml", "xml"),
          Arrays.asList("html", "svelte", "vue", "xaml", "xml")
        )
      ),
      Map.entry(
        Language.LANGUAGE_JAVA,
        new Configuration(Arrays.asList("java"), Arrays.asList("java"))
      ),
      Map.entry(
        Language.LANGUAGE_JAVASCRIPT,
        new Configuration(
          Arrays.asList("javascript", "typescript", "js", "ts"),
          Arrays.asList("js", "jsx", "ts", "tsx")
        )
      ),
      Map.entry(
        Language.LANGUAGE_KOTLIN,
        new Configuration(Arrays.asList("kotlin"), Arrays.asList("kt"), true)
      ),
      Map.entry(
        Language.LANGUAGE_PYTHON,
        new Configuration(Arrays.asList("python"), Arrays.asList("py"))
      ),
      Map.entry(
        Language.LANGUAGE_RUBY,
        new Configuration(Arrays.asList("ruby"), Arrays.asList("rb"))
      ),
      Map.entry(
        Language.LANGUAGE_RUST,
        new Configuration(Arrays.asList("rust"), Arrays.asList("rs"))
      ),
      Map.entry(
        Language.LANGUAGE_SCSS,
        new Configuration(
          Arrays.asList("css", "less", "sass", "scss"),
          Arrays.asList("css", "less", "scss")
        )
      )
    )
  );

  private Configuration empty = new Configuration(Arrays.asList(), Arrays.asList(""));

  @Inject
  public LanguageDeterminer() {}

  private List<Language> languagesMatching(
    Predicate<Map.Entry<Language, Configuration>> predicate
  ) {
    return configurations
      .entrySet()
      .stream()
      .filter(predicate)
      .map(configuration -> configuration.getKey())
      .collect(Collectors.toList());
  }

  public List<String> apiNames(Language language) {
    return configurations.getOrDefault(language, empty).apiNames;
  }

  public String enumString(Language language) {
    return language.toString().split("_")[1].toLowerCase();
  }

  public List<String> extensions(Language language) {
    return configurations.getOrDefault(language, empty).extensions;
  }

  public Language fromApiName(String name) {
    return configurations
      .entrySet()
      .stream()
      .filter(configuration -> configuration.getValue().apiNames.contains(name))
      .map(configuration -> configuration.getKey())
      .findFirst()
      .orElse(Language.LANGUAGE_DEFAULT);
  }

  public Language fromFilename(String filename) {
    String normalized = filename.toLowerCase();
    return configurations
      .entrySet()
      .stream()
      .filter(
        configuration ->
          configuration.getValue().extensions.stream().anyMatch(e -> normalized.endsWith("." + e))
      )
      .map(e -> e.getKey())
      .findFirst()
      .orElse(Language.LANGUAGE_DEFAULT);
  }

  public List<Language> languages() {
    return languagesMatching(e -> true);
  }

  public List<Language> languagesWithMlSnippetsDisabled() {
    return languagesMatching(configuration -> configuration.getValue().mlSnippetsDisabled);
  }
}
