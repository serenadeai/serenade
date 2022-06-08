package core.exception;

public class NoFileFound extends SafeToDisplayException {

  public NoFileFound() {
    super("No file found");
  }

  @Override
  public boolean displayTranscript() {
    return false;
  }
}
