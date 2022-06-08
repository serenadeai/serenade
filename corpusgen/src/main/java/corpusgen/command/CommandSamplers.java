package corpusgen.command;

import core.parser.MutableParseTree;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class CommandSamplers {

  public final HashMap<String, Supplier<List<MutableParseTree>>> ruleToSampler;
  public final List<String> textCommands;
  public final List<String> nonTextCommands;

  public CommandSamplers(
    HashMap<String, Supplier<List<MutableParseTree>>> ruleToSampler,
    List<String> textCommands,
    List<String> nonTextCommands
  ) {
    this.ruleToSampler = ruleToSampler;
    this.textCommands = textCommands;
    this.nonTextCommands = nonTextCommands;
  }
}
