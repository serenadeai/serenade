package core.exception;

public class CannotDetermineLanguage extends SafeToDisplayException {

  public CannotDetermineLanguage() {
    super("Cannot determine language from filename");
  }
}
