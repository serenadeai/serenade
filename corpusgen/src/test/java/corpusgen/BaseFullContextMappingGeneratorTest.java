package corpusgen.mapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

import core.codeengine.Resolver;
import core.codeengine.Tokenizer;
import core.formattedtext.ConversionMap;
import core.formattedtext.ConversionMapFactory;
import core.gen.rpc.Language;
import core.util.Diff;
import core.util.Range;
import core.util.Whitespace;
import corpusgen.CorpusGenComponent;
import corpusgen.DaggerCorpusGenComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

class ReadableMapping {

  public final String transcript;
  public final String source;
  public final int cursor;

  public ReadableMapping(String transcript, String source, int cursor) {
    this.transcript = transcript;
    this.source = source;
    this.cursor = cursor;
  }

  private String after() {
    if (0 <= cursor && cursor <= source.length()) {
      return source.substring(0, cursor) + "<>" + source.substring(cursor);
    }
    return source;
  }

  public String toString() {
    return ("[transcript=" + this.transcript + ", after=" + after() + ", cursor=" + cursor + "]");
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.transcript, this.source, this.cursor);
  }

  @Override
  public boolean equals(Object o) {
    if (this.getClass() != o.getClass()) {
      return false;
    }

    ReadableMapping r = (ReadableMapping) o;
    return (
      this.transcript.equals(r.transcript) &&
      this.source.equals(r.source) &&
      this.cursor == r.cursor
    );
  }
}

class Matches {

  public final List<ReadableMapping> candidates;
  public final List<ReadableMapping> transcriptMatches;
  public final List<ReadableMapping> sourceMatches;
  public final List<ReadableMapping> cursorMatches;

  public Matches(
    List<ReadableMapping> candidates,
    String transcript,
    String finalSource,
    int finalCursor
  ) {
    this.candidates = candidates;

    transcriptMatches =
      candidates.stream().filter(m -> m.transcript.equals(transcript)).collect(Collectors.toList());
    sourceMatches =
      transcriptMatches
        .stream()
        .filter(m -> m.source.equals(finalSource))
        .collect(Collectors.toList());
    cursorMatches =
      sourceMatches.stream().filter(m -> m.cursor == finalCursor).collect(Collectors.toList());
  }
}

public abstract class BaseFullContextMappingGeneratorTest {

  protected CorpusGenComponent component = DaggerCorpusGenComponent.create();
  ConversionMapFactory conversionMapFactory = component.conversionMapFactory();
  Resolver resolver = component.resolver();
  Tokenizer tokenizer = component.tokenizer();
  Whitespace whitespace = new Whitespace();

  protected Language language(String filename) {
    return component.languageDeterminer().fromFilename(filename);
  }

  protected Matches generateMatches(
    String initialSource,
    int initialCursor,
    String generationSource,
    Language language,
    String transcript,
    String finalSource,
    int finalCursor,
    int samples,
    Boolean includeAddMappings
  ) {
    Config config = new Config();
    config.language = language;
    config.sampleAlternativeWords = false;
    config.sampleFillerWords = false;
    config.includeAddMappings = includeAddMappings;

    FullContextMappingGenerator generator = component
      .fullContextMappingGeneratorFactory()
      .create(config);
    List<FullContextMapping> mappings = new ArrayList<>();
    for (int i = 0; i < samples; i++) {
      mappings.addAll(generator.generateMappings(generationSource, Optional.empty()));
    }
    // filter insert mappings for the right start.
    mappings =
      mappings
        .stream()
        .filter(m -> m.node.isPresent() || m.start == initialCursor)
        .collect(Collectors.toList());
    List<ReadableMapping> candidates = candidates(
      mappings,
      initialSource,
      initialCursor,
      generationSource,
      language
    );
    return new Matches(candidates, transcript, finalSource, finalCursor);
  }

  protected void assertMappingsContain(
    String initialSource,
    int initialCursor,
    String generationSource,
    Language language,
    String transcript,
    String finalSource,
    int finalCursor
  ) {
    Boolean includeAddMappings = true;
    if (transcript.startsWith("add")) {
      transcript = transcript.substring("add".length() + 1);
      includeAddMappings = true;
    } else if (transcript.startsWith("insert")) {
      transcript = transcript.substring("insert".length() + 1);
      includeAddMappings = false;
    }
    Matches matches = generateMatches(
      initialSource,
      initialCursor,
      generationSource,
      language,
      transcript,
      finalSource,
      finalCursor,
      500,
      includeAddMappings
    );
    ReadableMapping expected = new ReadableMapping(transcript, finalSource, finalCursor);
    if (matches.cursorMatches.size() != 0) {
      return;
    }
    matches =
      generateMatches(
        initialSource,
        initialCursor,
        generationSource,
        language,
        transcript,
        finalSource,
        finalCursor,
        10000,
        includeAddMappings
      );

    assertTrue(
      matches.transcriptMatches.size() != 0,
      "Couldn't find any mappings matching transcript for mapping:\n" +
      expected +
      "\n----\nMost frequent mappings:\n" +
      top(matches.candidates)
    );
    assertTrue(
      matches.sourceMatches.size() != 0,
      "Couldn't find any mappings matching source for mapping:\n" +
      expected +
      "\n----\nMost frequent matching transcript:\n" +
      top(matches.transcriptMatches)
    );
    assertTrue(
      matches.cursorMatches.size() != 0,
      "Couldn't find any mappings matching cursor for mapping:\n" +
      expected +
      "\n----\nMost frequent matching source:\n" +
      top(matches.sourceMatches)
    );
  }

  List<ReadableMapping> top(List<ReadableMapping> mappings) {
    return mappings
      .stream()
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
      .entrySet()
      .stream()
      .sorted(Map.Entry.<ReadableMapping, Long>comparingByValue().reversed())
      .limit(25)
      .map(e -> e.getKey())
      .collect(Collectors.toList());
  }

  List<ReadableMapping> candidates(
    List<FullContextMapping> mappings,
    String initialSource,
    int initialCursor,
    String generationSource,
    Language language
  ) {
    return mappings
      .stream()
      .map(m -> readableMapping(m, initialSource, initialCursor, generationSource, language))
      .collect(Collectors.toList());
  }

  protected ReadableMapping readableMapping(
    FullContextMapping mapping,
    String initialSource,
    int initialCursor,
    String generationSource,
    Language language
  ) {
    Range range;
    String source;
    if (mapping.node.isPresent()) {
      source = generationSource;
      range = mapping.node.get().range();
    } else {
      source = initialSource;
      range = new Range(mapping.start, mapping.start);
    }
    String generated = tokenizer.decodeModelCodeRepresentation(
      mapping.outputModelCodeRepresentation()
    );
    ConversionMap conversionMap = conversionMapFactory.create(language);
    Diff resolved = resolver.resolveReservedSlots(
      Diff.fromInitialState(source, 0),
      range,
      generated,
      conversionMap
    )
      .diff;
    return new ReadableMapping(mapping.transcript(), resolved.getSource(), resolved.getCursor());
  }
}
