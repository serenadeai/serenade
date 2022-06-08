package core.evaluator;

import core.gen.rpc.Change;
import core.gen.rpc.Command;
import core.gen.rpc.CommandsResponseAlternative;
import core.metadata.EditorStateWithMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NewlineNormalizer {

  public class Normalization {

    public final boolean addedNewline;
    public final EditorStateWithMetadata state;

    public Normalization(EditorStateWithMetadata state, boolean addedNewline) {
      this.addedNewline = addedNewline;
      this.state = state;
    }

    public CommandsResponseAlternative revert(CommandsResponseAlternative alternative) {
      return CommandsResponseAlternative
        .newBuilder(alternative)
        .clearCommands()
        .addAllCommands(
          alternative.getCommandsList().stream().map(c -> revert(c)).collect(Collectors.toList())
        )
        .build();
    }

    private Command revert(Command command) {
      String source = command.getSource();
      int cursor = command.getCursor();
      if (addedNewline && source.endsWith("\n") && cursor != source.length()) {
        source = source.substring(0, source.length() - 1);
      }

      List<Change> changes = new ArrayList<>(command.getChangesList());
      if (changes.size() > 0) {
        Change lastChange = changes.get(changes.size() - 1);
        if (lastChange.getStop() > state.getSource().length()) {
          changes.set(
            changes.size() - 1,
            Change.newBuilder(lastChange).setStop(state.getSource().length()).build()
          );
        }
      }

      return Command
        .newBuilder(command)
        .clearChanges()
        .addAllChanges(changes)
        .setSource(source)
        .build();
    }
  }

  @Inject
  public NewlineNormalizer() {}

  public Normalization normalize(EditorStateWithMetadata state) {
    state = new EditorStateWithMetadata(state);
    String[] segments = state.getSource().split("\r");
    int newCursor = state.getCursor();
    StringBuilder sb = new StringBuilder();
    for (String segment : segments) {
      sb.append(segment);
      if (sb.length() < newCursor) {
        newCursor--;
      }
    }
    // Most editors do this, sometimes secretly. Languages like python require it.
    boolean addedNewline = false;
    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
      sb.append("\n");
      addedNewline = true;
    }
    state.setSource(sb.toString());
    state.setCursor(newCursor);
    return new Normalization(state, addedNewline);
  }
}
