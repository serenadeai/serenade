package grammarflattener;

import dagger.Component;
import javax.inject.Singleton;

@Component(modules = { GrammarFlattenerModule.class })
@Singleton
public interface GrammarFlattenerComponent {
  public GrammarFlattener grammarFlattener();
}
