package core;

import core.gen.rpc.Language;
import core.server.CoreServer;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import toolbelt.client.ServiceHttpClient;
import toolbelt.logging.Logs;

public class Core {

  private static int port = Integer.parseInt(System.getenv("CORE_PORT"));
  private static Logger logger = LoggerFactory.getLogger(Core.class);

  public static void main(String[] args) {
    ArgumentParser parser = ArgumentParsers
      .newFor("Core")
      .build()
      .defaultHelp(true)
      .description("Core Serenade application");

    try {
      parser.parseArgs(args);
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.exit(1);
    }

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    CoreComponent component = DaggerCoreComponent.builder().build();
    CoreServer server = component.server();

    // eager-load expensive dependencies on server start rather than the first request
    component.streamManagerFactory().create(null);
    for (Language language : Language.values()) {
      component.selectorFactory().create(language);
    }

    Logs.logInfo(logger, "core.started", "Running core on " + port);
    server.start(port);
  }
}
