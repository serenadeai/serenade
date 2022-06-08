package corpusgen;

import codeengine.gen.rpc.Model;
import core.ast.api.AstSyntaxError;
import core.codeengine.Input;
import core.codeengine.Tokenizer;
import core.codeengine.UnknownReplacer;
import core.evaluator.TranscriptParser;
import core.gen.rpc.Language;
import core.metadata.EditorStateWithMetadata;
import core.parser.Grammar;
import core.parser.MutableParseTree;
import core.parser.ParseTree;
import core.util.FillerWords;
import corpusgen.command.Chainer;
import corpusgen.command.CommandGenerator;
import corpusgen.command.CommandSamplers;
import corpusgen.mapping.Config;
import corpusgen.mapping.FileFilter;
import corpusgen.mapping.FullContextMapping;
import corpusgen.mapping.FullContextMappingGenerator;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import speechengine.gen.rpc.Alternative;

public class CorpusGen {

  @Inject
  Chainer chainer;

  @Inject
  CommandGenerator commandGenerator;

  @Inject
  FileFilter fileFilter;

  @Inject
  FillerWords fillerWords;

  @Inject
  FullContextMappingGenerator.Factory fullContextMappingGeneratorFactory;

  @Inject
  Grammar grammar;

  @Inject
  Tokenizer tokenizer;

  @Inject
  public CorpusGen() {}

  public static void main(String[] args) throws IOException, InterruptedException {
    CorpusGen corpusGen = DaggerCorpusGenComponent.create().corpusGen();
    ArgumentParser argumentParser = ArgumentParsers
      .newFor("CorpusGen")
      .build()
      .defaultHelp(true)
      .description("Utilities for generating commands to run a language model on.");
    argumentParser.addArgument("language").type(String.class);
    Subparsers subparsers = argumentParser.addSubparsers().dest("mode");

    Subparser lexiconSubparser = subparsers
      .addParser("lexicon")
      .help(
        "Generates a lexicon for a code engine model so that core can substitute unknown" +
        " tokens before sending it off to code engine."
      );
    lexiconSubparser.addArgument("name").type(String.class);
    lexiconSubparser.addArgument("max-words").type(Integer.class);
    lexiconSubparser.addArgument("output-file").type(Arguments.fileType());

    Subparser textSubparser = subparsers
      .addParser("text")
      .help("Generates formatted text (roughly the input side of code engine).");
    textSubparser.addArgument("output-file").type(Arguments.fileType());

    Subparser showSubparser = subparsers
      .addParser("show")
      .help("Show sample transcripts of a particular grammar rule.");
    showSubparser.addArgument("rule").type(String.class);

    Subparser mappingSubparser = subparsers
      .addParser("mapping")
      .help(
        "Generate auto-style dataset. Inputs can be used to generate transcript parsing" +
        " dataset."
      );
    mappingSubparser.addArgument("model").type(String.class);
    mappingSubparser.addArgument("inputs-file").type(Arguments.fileType());
    mappingSubparser.addArgument("outputs-file").type(Arguments.fileType());
    mappingSubparser.addArgument("--disable-add").type(Boolean.class).action(Arguments.storeTrue());

    Subparser unknownsSubparser = subparsers
      .addParser("unknowns")
      .help("Replaces unknown words in a dataset with unknown tokens.");
    unknownsSubparser.addArgument("model").type(String.class);
    unknownsSubparser.addArgument("lexicon-file").type(Arguments.fileType());
    unknownsSubparser.addArgument("inputs-file").type(Arguments.fileType());
    unknownsSubparser.addArgument("outputs-file").type(Arguments.fileType());
    unknownsSubparser.addArgument("converted-inputs-file").type(Arguments.fileType());
    unknownsSubparser.addArgument("converted-outputs-file").type(Arguments.fileType());

    Namespace namespace = null;
    try {
      namespace = argumentParser.parseArgs(args);
    } catch (ArgumentParserException e) {
      argumentParser.handleError(e);
      System.exit(1);
    }

    String languageName = namespace.getString("language");
    Language language = Language.valueOf(
      Language.getDescriptor().findValueByName("LANGUAGE_" + languageName.toUpperCase())
    );
    String mode = namespace.getString("mode");

    if (mode.equals("mapping")) {
      String modelName = namespace.getString("model");
      Model model = Model.valueOf(
        Model.getDescriptor().findValueByName("MODEL_" + modelName.replace("-", "_").toUpperCase())
      );
      Config config = new Config();
      config.language = language;
      config.sampleAlternativeWords = false;
      config.sampleFillerWords = false;
      config.includeAddMappings = false;
      if (model.equals(Model.MODEL_AUTO_STYLE)) {
        config.includeAddMappings = !namespace.getBoolean("disable_add");
      }
      corpusGen.generateMapping(
        config,
        Optional.of(model),
        language,
        namespace.getString("inputs_file"),
        Optional.of(namespace.getString("outputs_file"))
      );
    } else if (mode.equals("lexicon")) {
      corpusGen.generateLexicon(
        language,
        namespace.getString("name"),
        namespace.getInt("max_words"),
        namespace.getString("output_file")
      );
    } else if (mode.equals("text")) {
      Config config = new Config();
      config.language = language;
      config.sampleAlternativeWords = true;
      config.sampleFillerWords = true;
      config.includeAddMappings = false;
      corpusGen.generateMapping(
        config,
        Optional.empty(),
        language,
        namespace.getString("output_file"),
        Optional.empty()
      );
    } else if (mode.equals("unknowns")) {
      String modelName = namespace.getString("model");
      Model model = Model.valueOf(
        Model.getDescriptor().findValueByName("MODEL_" + modelName.replace("-", "_").toUpperCase())
      );
      corpusGen.generateUnknowns(
        model,
        namespace.getString("lexicon_file"),
        namespace.getString("inputs_file"),
        namespace.getString("outputs_file"),
        namespace.getString("converted_inputs_file"),
        namespace.getString("converted_outputs_file")
      );
    } else if (mode.equals("show")) {
      corpusGen.showRule(namespace.getString("rule"), language);
    }
  }

  private <T> void applyInParallelToStandardInFiles(
    BiConsumer<String, String> generate,
    Object writeLock
  ) {
    int numThreads = Runtime.getRuntime().availableProcessors();
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    String line = new String();
    List<String> paths = new ArrayList<>();
    try {
      while ((line = reader.readLine()) != null) {
        paths.add(line);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    ExecutorService generatorExecutor = Executors.newFixedThreadPool(numThreads);
    ExecutorService shardExecutor = Executors.newFixedThreadPool(numThreads);

    List<Future<?>> shards = new ArrayList<Future<?>>();
    for (int j = 0; j < numThreads; j++) {
      int shard = j;
      shards.add(
        shardExecutor.submit(
          () -> {
            for (int i = shard; i < paths.size(); i += numThreads) {
              // Hack to make sure we don't trigger race conditions in our
              // dependency injection initialization. We have a lot of classes
              // with this initialization pattern that's not thread safe.
              if (i > 0 && i < numThreads) {
                try {
                  Thread.sleep(2000);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
              }
              try {
                if (i % 100 == shard) {
                  System.out.println(
                    new Date() +
                    "; Processing file " +
                    String.valueOf(i) +
                    "/" +
                    String.valueOf(paths.size()) +
                    "; Shard: " +
                    String.valueOf(shard)
                  );
                }
                String path = paths.get(i);
                byte[] encoded = Files.readAllBytes(Paths.get(path));
                String source = new String(encoded, Charset.defaultCharset());
                Future<Void> future = generatorExecutor.submit(
                  () -> {
                    generate.accept(path, source);
                    return null;
                  }
                );
                try {
                  future.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                  if (e instanceof TimeoutException) {
                    System.out.println("Timeout: " + paths.get(i));
                  }
                  // don't kill a process when it has the lock.
                  // not sure this actually works or matters.
                  synchronized (writeLock) {
                    future.cancel(true);
                  }
                  // handle other exceptions
                  System.out.println("couldn't process: " + paths.get(i));
                  e.printStackTrace();
                  continue;
                }
              } catch (IOException e) {
                System.out.println("couldn't open " + paths.get(i));
              }
            }
            System.out.println("Shard " + shard + " finished.");
          }
        )
      );
    }

    try {
      for (Future<?> f : shards) {
        f.get();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    generatorExecutor.shutdown();
    shardExecutor.shutdown();
  }

  private String samplePrefixedFillerWord(Config config, ParseTree parseTree) {
    String transcript = parseTree.getSource();
    if (config.sampleFillerWords) {
      // Sample filler words by themselves, or prefixed to a command (not metaCommand).
      List<ParseTree> commandNodes = parseTree.getChildren().get(0).getChildren();
      if (
        commandNodes.get(0).getType().equals("command") &&
        ThreadLocalRandom.current().nextDouble() < 0.05
      ) {
        transcript = fillerWords.sample() + " " + transcript;
      }
    }
    return transcript;
  }

  public void generateMapping(
    Config config,
    Optional<Model> codeEngineModelType,
    Language language,
    String inputsFile,
    Optional<String> outputsFile
  ) {
    FullContextMappingGenerator generator = fullContextMappingGeneratorFactory.create(config);
    CommandSamplers samplers = commandGenerator.generate(language);
    PrintWriter input;
    Optional<PrintWriter> output;
    try {
      input = new PrintWriter(inputsFile);
      output =
        outputsFile.isPresent()
          ? Optional.of(new PrintWriter(outputsFile.get()))
          : Optional.empty();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Object writeLock = new Object();
    applyInParallelToStandardInFiles(
      (filename, source) -> {
        List<FullContextMapping> mappings = Collections.emptyList();
        List<String> unprocessableReasons = Arrays.asList();
        try {
          unprocessableReasons = fileFilter.unprocessableReasons(filename, source, language);
          if (unprocessableReasons.size() == 0) {
            mappings = generator.generateMappings(source, Optional.empty());
          }
        } catch (AstSyntaxError e) {
          mappings = Collections.emptyList();
        }
        List<String> inputLines = new ArrayList<>();
        List<String> outputLines = new ArrayList<>();
        if (codeEngineModelType.isEmpty()) {
          List<FullContextMapping> remainingMappings = new ArrayList<>();
          for (FullContextMapping mapping : mappings) {
            if (ThreadLocalRandom.current().nextDouble() < 0.05) {
              inputLines.add(fillerWords.sample());
            }
            // simulate invalid transcripts some of the time with raw formatted text.
            if (ThreadLocalRandom.current().nextDouble() < 0.025) {
              inputLines.add(mapping.transcript());
            } else {
              remainingMappings.add(mapping);
            }
          }
          List<ParseTree> chains = chainer.generateChains(samplers, remainingMappings);
          for (ParseTree chain : chains) {
            inputLines.add(samplePrefixedFillerWord(config, chain));
          }
        } else if (codeEngineModelType.equals(Optional.of(Model.MODEL_AUTO_STYLE))) {
          for (FullContextMapping mapping : mappings) {
            inputLines.add(mapping.sampleInput().modelCodeRepresentation());
            outputLines.add(mapping.outputModelCodeRepresentation());
          }
        } else if (codeEngineModelType.equals(Optional.of(Model.MODEL_CONTEXTUAL_LANGUAGE_MODEL))) {
          for (FullContextMapping mapping : mappings) {
            inputLines.add(mapping.sampleInput().leadingContextRepresentation());
            outputLines.add(mapping.transcript());
          }
        } else if (codeEngineModelType.equals(Optional.of(Model.MODEL_TRANSCRIPT_PARSER))) {
          Collections.shuffle(mappings);
          List<ParseTree> chains = chainer.generateChains(samplers, mappings);
          inputLines.addAll(chains.stream().map(c -> c.getSource()).collect(Collectors.toList()));
          outputLines.addAll(
            chains
              .stream()
              .map(c -> c.toMarkup().stream().collect(Collectors.joining(" ")))
              .collect(Collectors.toList())
          );
        }
        synchronized (writeLock) {
          for (String line : inputLines) {
            input.println(line);
          }
          if (output.isPresent()) {
            for (String line : outputLines) {
              output.get().println(line);
            }
          }
        }
      },
      writeLock
    );
    input.flush();
    output.ifPresent(o -> o.flush());
    input.close();
    output.ifPresent(o -> o.close());
    System.out.println("Done generating mappings.");
  }

  public void generateLexicon(Language language, String name, int maxWords, String outputFile)
    throws IOException {
    Set<String> lexicon = new HashSet<>();
    Scanner in = new Scanner(System.in);
    PrintWriter output = new PrintWriter(outputFile);
    Iterable<String> it = () -> new Scanner(System.in);
    Stream<String> lines = StreamSupport.stream(it.spliterator(), false);

    List<String> orderedTokensByFrequency = lines
      .flatMap(line -> Stream.of(line.split(" ")))
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
      .entrySet()
      .stream()
      .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
      .limit(maxWords)
      .map(e -> e.getKey())
      .collect(Collectors.toList());

    for (String word : orderedTokensByFrequency) {
      // We need to add these one by one because tokens here might have shown up already in the
      // whitelist.
      if (lexicon.size() > maxWords) {
        break;
      }
      if (!lexicon.contains(word)) {
        lexicon.add(word);
      }
    }
    lexicon.stream().forEach(w -> output.println(w));
    output.flush();
  }

  public void generateUnknowns(
    Model model,
    String lexiconFile,
    String inputsFile,
    String outputsFile,
    String convertedInputsFile,
    String convertedOutputsFile
  ) throws IOException {
    BufferedReader lexiconReader = new BufferedReader(new FileReader(lexiconFile));
    BufferedReader inputReader = new BufferedReader(new FileReader(inputsFile));
    BufferedReader outputReader = new BufferedReader(new FileReader(outputsFile));
    String inputLine = new String();
    PrintWriter input = new PrintWriter(convertedInputsFile);
    PrintWriter output = new PrintWriter(convertedOutputsFile);

    Set<String> lexicon = new HashSet<>();
    while ((inputLine = lexiconReader.readLine()) != null) {
      lexicon.add(inputLine.trim());
    }
    UnknownReplacer unknownReplacer = new UnknownReplacer(lexicon);

    if (model.equals(Model.MODEL_AUTO_STYLE) || model.equals(Model.MODEL_TRANSCRIPT_PARSER)) {
      while ((inputLine = inputReader.readLine()) != null) {
        String outputLine = outputReader.readLine();
        UnknownReplacer.StringsWithUnknowns inputWithUnknowns;
        String outputWithInputReplacements;
        try {
          inputWithUnknowns = unknownReplacer.stringsWithUnknowns(Arrays.asList(inputLine));
          List<String> additionalUnknowns = unknownReplacer.additionalUnknowns(
            inputWithUnknowns,
            outputLine
          );
          // This usually happens when we have an implicit section, but the implicit word isn't
          // in the lexicon, so there isn't anything we can do with these results if the model
          // predicts them.
          if (additionalUnknowns.size() > 0) {
            continue;
          }
          outputWithInputReplacements =
            unknownReplacer.replaceUnknowns(outputLine, inputWithUnknowns.unknowns);
        } catch (UnknownReplacer.HitMaxUnknowns e) {
          System.out.println("Max unknowns, skipping: " + outputLine);
          continue;
        }
        input.println(inputWithUnknowns.strings.get(0));
        output.println(outputWithInputReplacements);
      }
    } else if (model.equals(Model.MODEL_CONTEXTUAL_LANGUAGE_MODEL)) {
      // Note: since we are using SentencePiece, we don't want to do UNK replacement.
      while ((inputLine = inputReader.readLine()) != null) {
        String outputLine = outputReader.readLine();
        input.println(inputLine);
        output.println(outputLine);
      }
    }
    input.flush();
    output.flush();
  }

  public void showRule(String rule, Language language) {
    CommandSamplers samplers = commandGenerator.generate(language);
    for (int i = 0; i < 10; i++) {
      System.out.println(
        samplers.ruleToSampler
          .get(rule)
          .get()
          .get(0)
          .toParseTree()
          .toMarkup()
          .stream()
          .collect(Collectors.joining(" "))
      );
    }
  }
}
