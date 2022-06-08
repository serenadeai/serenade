package core.commands;

import core.language.LanguageSpecificFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CommandsFactory extends LanguageSpecificFactory<Commands> {

  @Inject
  BashCommands bashCommands;

  @Inject
  CPlusPlusCommands cPlusPlusCommands;

  @Inject
  CSharpCommands cSharpCommands;

  @Inject
  DartCommands dartCommands;

  @Inject
  Commands defaultCommands;

  @Inject
  GoCommands goCommands;

  @Inject
  HtmlCommands htmlCommands;

  @Inject
  JavaCommands javaCommands;

  @Inject
  JavaScriptCommands javaScriptCommands;

  @Inject
  KotlinCommands kotlinCommands;

  @Inject
  PythonCommands pythonCommands;

  @Inject
  RubyCommands rubyCommands;

  @Inject
  RustCommands rustCommands;

  @Inject
  ScssCommands scssCommands;

  @Inject
  public CommandsFactory() {}

  @Override
  public Optional<Commands> defaultValue() {
    return Optional.of(defaultCommands);
  }

  @Override
  protected List<Commands> elements() {
    return Arrays.asList(
      bashCommands,
      cPlusPlusCommands,
      cSharpCommands,
      dartCommands,
      goCommands,
      htmlCommands,
      javaCommands,
      javaScriptCommands,
      kotlinCommands,
      pythonCommands,
      rubyCommands,
      rustCommands,
      scssCommands
    );
  }
}
