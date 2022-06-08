package core.commands;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RubyCommands extends Commands {

  @Inject
  public RubyCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_RUBY;
  }
}
