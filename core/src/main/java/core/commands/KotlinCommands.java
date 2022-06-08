package core.commands;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KotlinCommands extends Commands {

  @Inject
  public KotlinCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_KOTLIN;
  }
}
