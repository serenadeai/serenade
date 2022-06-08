package core.metadata;

import com.google.common.base.MoreObjects;
import core.codeengine.SlotContext;
import core.evaluator.ParsedTranscript;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponseAlternative;
import core.gen.rpc.ErrorCode;
import core.util.Diff;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandsResponseAlternativeWithMetadata {

  public EditorStateWithMetadata state;
  public ParsedTranscript parsed;
  public String alternativeId;
  public List<Command> commands = new ArrayList<>();
  public Optional<String> remaining = Optional.empty();
  public Optional<String> description = Optional.empty();
  public Optional<Double> autoStyleCost = Optional.empty();
  public Optional<Double> contextualLanguageModelCost = Optional.empty();
  public Optional<SlotContext> slotContext = Optional.empty();
  public Optional<ErrorCode> errorCode = Optional.empty();

  public CommandsResponseAlternativeWithMetadata(
    EditorStateWithMetadata state,
    ParsedTranscript parsed
  ) {
    this.state = state;
    this.parsed = parsed;
    this.alternativeId = UUID.randomUUID().toString();
  }

  public CommandsResponseAlternativeWithMetadata(CommandsResponseAlternativeWithMetadata other) {
    this(other.state, other.parsed);
    this.commands = new ArrayList<>(other.commands);
    this.remaining = other.remaining;
    this.description = other.description;
    this.autoStyleCost = other.autoStyleCost;
    this.contextualLanguageModelCost = other.contextualLanguageModelCost;
    this.slotContext = other.slotContext;
    this.errorCode = other.errorCode;
  }

  public CommandsResponseAlternativeWithMetadata(
    EditorStateWithMetadata state,
    ParsedTranscript parsed,
    List<Command> commands
  ) {
    this(state, parsed);
    this.commands = commands;
  }

  public CommandsResponseAlternativeWithMetadata(
    EditorStateWithMetadata state,
    ParsedTranscript parsed,
    List<Command> commands,
    String description
  ) {
    this(state, parsed, commands);
    this.description = Optional.of(description);
  }

  public CommandsResponseAlternativeWithMetadata(
    EditorStateWithMetadata state,
    ParsedTranscript parsed,
    DiffWithMetadata diff
  ) {
    this(state, parsed);
    this.commands = new ArrayList<>(Arrays.asList(diff.toCommand()));
    this.autoStyleCost = diff.autoStyleCost;
    this.contextualLanguageModelCost = diff.contextualLanguageModelCost;
    this.slotContext = diff.slotContext;
  }

  public CommandsResponseAlternativeWithMetadata(
    EditorStateWithMetadata state,
    ParsedTranscript parsed,
    DiffWithMetadata diff,
    String description
  ) {
    this(state, parsed, diff);
    this.description = Optional.of(description);
  }

  @Override
  public boolean equals(Object other) {
    if (other.getClass() != getClass()) {
      return false;
    }

    var o = (CommandsResponseAlternativeWithMetadata) other;
    return (
      state.equals(o.state) &&
      commands.equals(o.commands) &&
      parsed.equals(o.parsed) &&
      remaining.equals(o.remaining) &&
      description.equals(o.description) &&
      errorCode.equals(o.errorCode)
    );
  }

  public EditorStateWithMetadata finalState() {
    EditorStateWithMetadata state = new EditorStateWithMetadata(this.state);
    for (Command command : commands) {
      if (command.getType() != CommandType.COMMAND_TYPE_DIFF) {
        continue;
      }

      state.setSource(command.getSource());
      state.setCursor(command.getCursor());
    }

    return state;
  }

  public CommandsResponseAlternative toCommandsResponseAlternative() {
    return CommandsResponseAlternative
      .newBuilder()
      .addAllCommands(commands)
      .setAlternativeId(alternativeId)
      .setTranscript(parsed.transcript())
      .setDescription(description.orElse(""))
      .setRemaining(remaining.orElse(""))
      .setErrorCode(errorCode.orElse(ErrorCode.ERROR_CODE_NONE))
      .build();
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("state", state)
      .add("commands", commands)
      .add("parsed", parsed)
      .add("remaining", remaining)
      .add("description", description)
      .add("autoStyleCost", autoStyleCost)
      .add("contextualLanguageModelCost", contextualLanguageModelCost)
      .add("slotContext", slotContext)
      .add("errorCode", errorCode)
      .toString();
  }
}
