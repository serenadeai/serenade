package core.exception;

public class NextObjectNotFound extends SafeToDisplayException {

  public NextObjectNotFound() {
    super("Already at object, next not found");
  }
}
