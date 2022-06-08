package core.exception;

import core.util.ArrowKeyDirection;

public class CannotGoInDirection extends SafeToDisplayException {

  public CannotGoInDirection(ArrowKeyDirection direction) {
    super("Cannot go " + direction.name().toLowerCase() + " any more");
  }
}
