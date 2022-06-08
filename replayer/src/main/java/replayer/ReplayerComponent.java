package replayer;

import core.CoreModule;
import core.CoreSharedModule;
import dagger.Component;
import javax.inject.Singleton;

@Component(modules = { CoreModule.class, CoreSharedModule.class })
@Singleton
public interface ReplayerComponent {
  public Replayer.Factory replayerFactory();
}
