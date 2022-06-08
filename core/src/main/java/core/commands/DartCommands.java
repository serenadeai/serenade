package core.commands;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DartCommands extends Commands {

  @Inject
  public DartCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_DART;
  }
}
