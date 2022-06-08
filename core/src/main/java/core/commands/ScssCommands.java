package core.commands;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ScssCommands extends Commands {

  @Inject
  public ScssCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_SCSS;
  }
}
