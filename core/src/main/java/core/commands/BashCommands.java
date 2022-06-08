package core.commands;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BashCommands extends Commands {

  @Inject
  public BashCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_BASH;
  }
}
