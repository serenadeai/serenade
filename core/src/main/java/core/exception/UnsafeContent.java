package core.exception;

public class UnsafeContent extends SafeToDisplayException {

  public UnsafeContent() {
    super("Unsafe content detected in response");
  }
}
