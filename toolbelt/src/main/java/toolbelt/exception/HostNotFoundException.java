package toolbelt.exception;

public class HostNotFoundException extends RuntimeException {

  public HostNotFoundException(String host) {
    super("Host not found for: " + host);
  }
}
