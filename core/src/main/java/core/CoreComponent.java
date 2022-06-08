package core;

import core.selector.Selector;
import core.server.CoreServer;
import core.streaming.StreamManager;
import dagger.Component;
import javax.inject.Singleton;
import toolbelt.client.ServiceHttpClient;

@Component(modules = { CoreModule.class, CoreSharedModule.class })
@Singleton
public interface CoreComponent {
  public CoreServer server();

  public Selector.Factory selectorFactory();

  public StreamManager.Factory streamManagerFactory();

  public ServiceHttpClient serviceHttpClient();
}
