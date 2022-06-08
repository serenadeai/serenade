package core.evaluator;

import codeengine.gen.rpc.TranslationAlternative;
import core.codeengine.CodeEngineClient;
import core.gen.antlr.command.CommandLexer;
import core.gen.antlr.command.CommandParser;
import core.metadata.EditorStateWithMetadata;
import core.parser.CommandAntlrParser;
import core.parser.Grammar;
import core.parser.MutableParseTreeFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speechengine.gen.rpc.Alternative;
import toolbelt.logging.Logs;

@Singleton
public class TranscriptParser {

  private Logger logger = LoggerFactory.getLogger(TranscriptParser.class);
  private static Pattern reparseDictatePattern = Pattern.compile(
    "^(" +
    "(stop (insert|dictate|type)( mode)?)|" +
    "((insert|dictate|type) mode off)|" +
    "(stop dictating)|" +
    "(normal mode)|" +
    "(command mode)|" +
    "(pause)|(stop listening)|" +
    "(use)|(one)|(two)|(three)|(four)|(five)|(six)|(seven)|(eight)|(nine)|(ten)|" +
    "(undo)|(redo)|" +
    "(delete)|" +
    "(go to)|(change)|(repeat)|" +
    "(press)|(tab)|(enter)|(close)" +
    "(beginning)|(end)" +
    ").*"
  );

  @Inject
  CommandAntlrParser commandAntlrParser;

  @Inject
  Grammar grammar;

  @Inject
  CodeEngineClient codeEngineClient;

  @Inject
  MutableParseTreeFactory mutableParseTreeFactory;

  @Inject
  Reranker reranker;

  @Inject
  TranscriptNormalizer transcriptNormalizer;

  private static class ValidityLevelErrorListener extends BaseErrorListener {

    private Optional<Integer> syntaxErrorCharPositionInLine = Optional.empty();
    private String transcript;

    public ValidityLevelErrorListener(String transcript) {
      this.transcript = transcript;
    }

    public boolean isValid() {
      return syntaxErrorCharPositionInLine.isEmpty();
    }

    public void syntaxError(
      Recognizer<?, ?> recognizer,
      Object offendingSymbol,
      int line,
      int charPositionInLine,
      String msg,
      RecognitionException e
    ) {
      syntaxErrorCharPositionInLine = Optional.of(charPositionInLine);
    }
  }

  private final int maxParsed = 20;
  private final int maxValid = 10;

  @Inject
  public TranscriptParser() {}

  private ParsedTranscript parseWithAntlr(Alternative alternative, boolean prepositions) {
    alternative =
      transcriptNormalizer
        .normalize(Arrays.asList(alternative))
        .stream()
        .findFirst()
        .orElse(Alternative.newBuilder().build());

    String transcript = alternative.getTranscript();

    CommonTokenStream tokenStream = getTokenStream(transcript);
    ValidityLevelErrorListener errorListener = new ValidityLevelErrorListener(transcript);
    CommandParser parser = new CommandParser(tokenStream);
    parser.removeErrorListeners();
    parser.addErrorListener(errorListener);

    ParseTree tree = prepositions ? parser.main() : parser.mainWithoutPrepositions();
    core.parser.ParseTree convertedTree = commandAntlrParser.convertAntlrTree(
      transcript,
      tokenStream,
      tree
    );

    return new ParsedTranscript(
      Alternative.newBuilder(alternative).setTranscript(transcript).build(),
      errorListener.isValid(),
      convertedTree
    );
  }

  private CommonTokenStream getTokenStream(String transcript) {
    CharStream input = CharStreams.fromString(transcript);
    CommandLexer lexer = new CommandLexer(input);
    return new CommonTokenStream(lexer);
  }

  public boolean shouldReparseInDictate(String transcript) {
    return reparseDictatePattern.matcher(transcript).matches();
  }

  private List<ParsedTranscript> parseAndFilter(
    List<Alternative> alternatives,
    EditorStateWithMetadata state
  ) {
    List<String> transcripts = alternatives
      .stream()
      .map(e -> e.getTranscript())
      .collect(Collectors.toList());

    return Logs.logTime(
      logger,
      "core.parse-transcript",
      Map.of("transcripts", transcripts),
      () -> {
        List<List<TranslationAlternative>> codeEngineTranslationResponse = codeEngineClient.transcriptParserTranslate(
          transcripts
        );

        Set<String> seen = new HashSet<>();
        int invalid = 0;
        List<ParsedTranscript> result = new ArrayList<>();
        for (int i = 0; i < alternatives.size(); i++) {
          Alternative alternative = alternatives.get(i);
          List<ParsedTranscript> inner = new ArrayList<>();
          Optional<ParsedTranscript> firstInvalid = Optional.empty();
          for (TranslationAlternative translationAlternative : codeEngineTranslationResponse.get(
            i
          )) {
            // deduplicate markup.
            if (seen.contains(translationAlternative.getSentence())) {
              continue;
            }

            seen.add(translationAlternative.getSentence());
            ParsedTranscript parsed = parsedTranscript(alternative, translationAlternative);

            if (parsed.isValid) {
              inner.add(parsed);
            } else if (firstInvalid.isEmpty()) {
              firstInvalid = Optional.of(parsed);
            }
          }

          // Only add invalid alternatives if there isn't a valid one for this transcript.
          if (inner.size() == 0 && firstInvalid.isPresent()) {
            invalid++;
            result.add(firstInvalid.get());
          } else {
            result.addAll(inner);
          }

          if (result.size() - invalid >= maxValid) {
            break;
          }
        }

        return result;
      }
    );
  }

  private List<Alternative> collectUniqueAlternatives(
    List<Alternative> alternatives,
    Function<Alternative, List<Alternative>> extractor
  ) {
    Set<String> seenTranscripts = new HashSet<>();
    List<Alternative> result = new ArrayList<>();
    for (Alternative e : alternatives) {
      if (result.size() < maxParsed && !seenTranscripts.contains(e.getTranscript())) {
        result.addAll(extractor.apply(e));
      }

      seenTranscripts.add(e.getTranscript());
    }

    return result.stream().limit(maxParsed).collect(Collectors.toList());
  }

  private List<Alternative> substituteCustomWords(
    List<Alternative> alternatives,
    EditorStateWithMetadata state
  ) {
    Map<String, String> customWords = state.getCustomWords();
    return collectUniqueAlternatives(
      alternatives,
      e -> {
        String transcript = e.getTranscript();
        for (String key : customWords.keySet()) {
          transcript = transcript.replaceAll("\\b" + key + "\\b", customWords.get(key));
        }

        return Arrays.asList(
          Alternative
            .newBuilder(e)
            .setTranscript(transcript)
            .setTranscriptId(e.getTranscriptId())
            .build()
        );
      }
    );
  }

  private List<Alternative> addDictateCommands(
    List<Alternative> alternatives,
    EditorStateWithMetadata state
  ) {
    return collectUniqueAlternatives(
      alternatives,
      e -> {
        if (state.getDictateMode()) {
          List<Alternative> result = new ArrayList<>();
          // In dictate mode, only parse the transcript itself if it matches
          // a heuristic indicating that it is intended to be a normal command
          if (shouldReparseInDictate(e.getTranscript())) {
            result.add(e);
          }

          result.add(
            Alternative
              .newBuilder(e)
              .setTranscript("insert " + e.getTranscript())
              .setTranscriptId("DICTATE_MODE_" + e.getTranscriptId())
              .build()
          );

          return result;
        }

        return Arrays.asList(e);
      }
    );
  }

  public List<ParsedTranscript> parseWithAntlr(
    Alternative alternative,
    EditorStateWithMetadata state
  ) {
    List<ParsedTranscript> result = new ArrayList<>();
    String transcript = alternative.getTranscript();

    // prioritize the interpreting prepositions as directions first, then as text
    // prepositions are only supported in apps with plugins
    Optional<ParsedTranscript> parsed = Optional.empty();
    if (state.getPluginInstalled()) {
      parsed = Optional.of(parseWithAntlr(alternative, true));
      result.add(parsed.get());
    }

    if (
      !state.getPluginInstalled() || (transcript.contains("after") || transcript.contains("before"))
    ) {
      ParsedTranscript parsedWithoutPrepositions = parseWithAntlr(alternative, false);

      // if we don't have a parse at all for this transcript yet, make sure it's included
      // otherwise, only include if it's valid and different than the first parse
      if (
        parsed.isEmpty() ||
        (
          parsedWithoutPrepositions.isValid &&
          parsedWithoutPrepositions.children().size() != parsed.get().children().size()
        )
      ) {
        result.add(parsedWithoutPrepositions);
      }
    }

    return result;
  }

  public List<ParsedTranscript> parse(
    List<Alternative> alternatives,
    EditorStateWithMetadata state,
    boolean rerank
  ) {
    if (rerank) {
      alternatives = reranker.rerankTranscripts(alternatives, state);
    }

    alternatives = transcriptNormalizer.normalize(alternatives);
    alternatives = substituteCustomWords(alternatives, state);
    alternatives = addDictateCommands(alternatives, state);

    List<ParsedTranscript> result = parseAndFilter(alternatives, state);
    return rerank ? reranker.rerankParsedTranscripts(result) : result;
  }

  public ParsedTranscript parsedTranscript(
    Alternative alternative,
    TranslationAlternative translation
  ) {
    double modelCost = (double) -translation.getScore();
    Alternative alternativeWithCost = Alternative.newBuilder(alternative).build();
    String transcript = alternative.getTranscript();

    Optional<core.parser.ParseTree> tree = mutableParseTreeFactory
      .create(Arrays.asList(translation.getSentence().split(" ")))
      .filter(mutableParseTrees -> mutableParseTrees.size() == 1)
      .map(mutableParseTrees -> mutableParseTrees.get(0).toParseTree())
      .filter(parseTree -> parseTree.getSource().equals(transcript));

    if (tree.isPresent() && grammar.matchesGrammar(tree.get())) {
      return new ParsedTranscript(alternative, true, tree.get(), Optional.of(modelCost));
    } else {
      return new ParsedTranscript(
        alternative,
        false,
        new core.parser.ParseTree("", "", "", 0, 0, Optional.empty()),
        Optional.of(modelCost)
      );
    }
  }
}
