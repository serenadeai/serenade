package replayer;

import java.util.ArrayList;
import java.util.List;

public class LoggedEditorState {

  public int cursor = 0;
  public List<LoggedCustomCommand> custom_commands = new ArrayList<>();
  public String source = "";
}
