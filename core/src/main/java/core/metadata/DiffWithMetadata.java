package core.metadata;

import com.google.common.base.MoreObjects;
import core.codeengine.SlotContext;
import core.gen.rpc.Change;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.util.Diff;
import java.util.Optional;
import java.util.stream.Collectors;

public class DiffWithMetadata {

  public Diff diff;
  public Optional<String> codeForDescription = Optional.empty();
  public Optional<String> codeToBeReplacedForDescription = Optional.empty();
  public Optional<String> nameForDescription = Optional.empty();
  public Optional<Double> autoStyleCost = Optional.empty();
  public Optional<Double> contextualLanguageModelCost = Optional.empty();
  public Optional<SlotContext> slotContext = Optional.empty();

  public DiffWithMetadata(Diff diff) {
    this.diff = diff;
  }

  public DiffWithMetadata(Diff diff, String codeForDescription) {
    this.diff = diff;
    this.codeForDescription = Optional.of(codeForDescription);
  }

  public Command toCommand() {
    return Command
      .newBuilder()
      .setType(CommandType.COMMAND_TYPE_DIFF)
      .setSource(diff.getSource())
      .setCursor(diff.getCursor())
      .clearChanges()
      .addAllChanges(
        diff
          .getChanges()
          .stream()
          .map(
            c ->
              Change
                .newBuilder()
                .setStart(c.range.start)
                .setStop(c.range.stop)
                .setSubstitution(c.substitution)
                .build()
          )
          .collect(Collectors.toList())
      )
      .build();
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("diff", diff)
      .add("codeForDescription", codeForDescription)
      .add("nameForDescription", nameForDescription)
      .add("autoStyleCost", autoStyleCost)
      .add("contextualLanguageModelCost", contextualLanguageModelCost)
      .toString();
  }
}
