package core.exception;

public class TimeoutExceeded extends SafeToDisplayException {

  public TimeoutExceeded() {
    super("Timeout exceeded");
  }
}
