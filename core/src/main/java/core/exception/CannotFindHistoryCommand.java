package core.exception;

public class CannotFindHistoryCommand extends SafeToDisplayException {

  public CannotFindHistoryCommand() {
    super("Cannot find command");
  }
}
