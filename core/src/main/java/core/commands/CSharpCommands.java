package core.commands;

import core.gen.rpc.Language;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CSharpCommands extends Commands {

  @Inject
  public CSharpCommands() {}

  @Override
  public Language language() {
    return Language.LANGUAGE_CSHARP;
  }
}
