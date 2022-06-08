package core.commands;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GoCommands extends Commands {

  @Inject
  public GoCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_GO;
  }
}
