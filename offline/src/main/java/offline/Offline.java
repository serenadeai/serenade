package offline;

import core.gen.rpc.Language;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparsers;
import offline.subcommands.BenchmarkRunner;
import offline.subcommands.TreePrinter;
import offline.subcommands.TutorialGenerator;

public class Offline {

  public static void main(String[] args) {
    OfflineComponent component = DaggerOfflineComponent.builder().build();
    Map<List<String>, Subcommand> subcommands = new HashMap<>() {
      {
        put(Arrays.asList("benchmark"), component.benchmarkRunner());
        put(Arrays.asList("debug-trees", "print-tree"), component.treePrinter());
        put(Arrays.asList("generate-tutorial"), component.tutorialGenerator());
      }
    };

    ArgumentParser parser = ArgumentParsers
      .newFor("Offline")
      .build()
      .defaultHelp(true)
      .description("Offline scripts");

    Subparsers subparsers = parser.addSubparsers().dest("command");
    for (Subcommand subcommand : subcommands.values()) {
      subcommand.configureSubparsers(subparsers);
    }

    Namespace namespace = null;
    try {
      namespace = parser.parseArgs(args);
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.exit(1);
    }

    String command = namespace.getString("command");
    for (Map.Entry<List<String>, Subcommand> entry : subcommands.entrySet()) {
      if (entry.getKey().contains(command)) {
        entry.getValue().run(namespace);
      }
    }
  }
}
