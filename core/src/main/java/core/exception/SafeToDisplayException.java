package core.exception;

public class SafeToDisplayException extends RuntimeException {

  public SafeToDisplayException(String message) {
    super(message);
  }

  public boolean displayTranscript() {
    return true;
  }
}
