package corpusgen.mapping;

import core.ast.Ast;
import core.ast.api.AstNode;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SnippetGenerator {

  private List<String> namePrefixes = Arrays.asList("with name", "called", "named");

  private List<Character> vowels = Arrays.asList('a', 'e', 'i', 'o', 'u');

  protected BiFunction<GenerationContext, Ast.Identifier, Mapping> generateName;
  protected MultiNodeSectionGenerator multiNodeSectionGenerator;
  protected Random random;

  @AssistedInject
  public SnippetGenerator(
    MultiNodeSectionGenerator multiNodeSectionGenerator,
    Random random,
    @Assisted BiFunction<GenerationContext, Ast.Identifier, Mapping> generateName
  ) {
    this.multiNodeSectionGenerator = multiNodeSectionGenerator;
    this.random = random;
    this.generateName = generateName;
  }

  @AssistedFactory
  public interface Factory {
    SnippetGenerator create(BiFunction<GenerationContext, Ast.Identifier, Mapping> generateName);
  }

  private Function<List<List<String>>, List<String>> aggregate(
    List<Integer> phraseIndices,
    List<String> phrases
  ) {
    return sectionPhrases -> {
      List<List<String>> allPhrases = new ArrayList<>(sectionPhrases);
      for (int i = 0; i < phraseIndices.size(); i++) {
        allPhrases.add(phraseIndices.get(i), Arrays.asList(phrases.get(i)));
      }

      List<String> result = allPhrases
        .stream()
        .flatMap(e -> e.stream())
        .filter(e -> !e.equals("")) // unclear why this happens.
        .collect(Collectors.toList());

      // if you used a phrase, then prefix with "a" or "an"
      if (phraseIndices.size() > 0 && random.bool(0.01)) {
        boolean useAn = vowels.contains(result.get(0).charAt(0));
        if (random.bool(0.05)) {
          useAn = !useAn;
        }
        result.add(0, useAn ? "an" : "a");
      }

      return result;
    };
  }

  private List<Section> sampleImplicitPrefix(List<Section> sections) {
    if (random.bool(0.9)) {
      return sections;
    }
    return sections
      .stream()
      .filter(e -> e instanceof NameSection)
      .findFirst()
      .map(
        e -> {
          int index = sections.indexOf(e);
          List<Section> updatedSections = new ArrayList<>();
          for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i) instanceof NodeSection) {
              updatedSections.add(((NodeSection) sections.get(i)).setImplicit(true));
            } else {
              updatedSections.add(sections.get(i));
            }
          }
          return updatedSections;
        }
      )
      .orElse(sections);
  }

  public <T extends AstNode> Mapping generate(
    GenerationContext ctx,
    T node,
    List<Section> sections
  ) {
    return generate(ctx, node, sections, n -> Optional.empty());
  }

  public <T extends AstNode> Mapping generate(
    GenerationContext ctx,
    T node,
    List<Section> sections,
    Function<T, Optional<Integer>> defaultCursor
  ) {
    return generate(ctx, node, sections, defaultCursor, false);
  }

  public <T extends AstNode> Mapping generate(
    GenerationContext ctx,
    T node,
    List<Section> sections,
    Function<T, Optional<Integer>> defaultCursor,
    boolean defaultCursorIfNoPartialSections
  ) {
    // Reduce all of the various types of sections to a list of NodeSections.
    Optional<Integer> nameIndex = sections
      .stream()
      .filter(e -> e instanceof NameSection)
      .findFirst()
      .map(e -> sections.indexOf(e));
    String namePhrase = null;
    List<Section> resolvedSections = new ArrayList<>();
    for (int i = 0; i < sections.size(); i++) {
      Section section = sections.get(i);
      if (section instanceof NameSection) {
        NameSection nameSection = (NameSection) section;
        nameIndex = Optional.of(i);
        namePhrase = nameSection.phrase;
        resolvedSections.add(new NodeSection<>(nameSection.node).setGenerate(generateName));
      } else {
        resolvedSections.add(section);
      }
    }

    if (nameIndex.isPresent()) {
      // Some of the time, say things like "function foo" where "public int" is implicit.
      if (random.bool(0.1)) {
        for (int i = 0; i < nameIndex.get(); i++) {
          if (resolvedSections.get(i) instanceof NodeSection) {
            resolvedSections.set(i, ((NodeSection) resolvedSections.get(i)).setImplicit(true));
          }
        }
      }

      double p = ThreadLocalRandom.current().nextDouble();
      if (p < 0.33) {
        // e.g. function int foo
        resolvedSections.add(0, new PhraseSection(namePhrase));
      } else if (p < 0.66) {
        // e.g. int function called foo, int function foo
        namePhrase += random.bool(0.97) ? "" : " " + random.element(namePrefixes);
        resolvedSections.add(nameIndex.get(), new PhraseSection(namePhrase));
      } else {
        // e.g. int foo function
        resolvedSections.add(nameIndex.get() + 1, new PhraseSection(namePhrase));
      }
    }

    List<PhraseSection> phraseSections = resolvedSections
      .stream()
      .filter(e -> e instanceof PhraseSection)
      .map(e -> (PhraseSection) e)
      .collect(Collectors.toList());
    List<String> phrases = phraseSections.stream().map(e -> e.phrase).collect(Collectors.toList());
    List<Integer> phraseIndices = phraseSections
      .stream()
      .map(e -> resolvedSections.indexOf(e))
      .collect(Collectors.toList());
    List<NodeSection<?>> nodeSections = resolvedSections
      .stream()
      .filter(e -> e instanceof NodeSection)
      .map(e -> (NodeSection<?>) e)
      .collect(Collectors.toList());

    int requiredCount = phraseIndices.size() > 0
      ? phraseIndices.get(phraseIndices.size() - 1) - (phraseIndices.size() - 1)
      : 0;
    return multiNodeSectionGenerator.generate(
      ctx,
      node,
      nodeSections,
      aggregate(phraseIndices, phrases),
      defaultCursor,
      defaultCursorIfNoPartialSections,
      requiredCount
    );
  }
}
