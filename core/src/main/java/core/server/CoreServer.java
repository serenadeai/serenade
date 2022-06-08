package core.server;

import core.streaming.StreamManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolbelt.logging.Logs;
import toolbelt.server.ApiErrorHandler;
import toolbelt.server.StatusServlet;

@Singleton
public class CoreServer {

  // set a 10MB limit on messages
  private int messageSizeLimit = 10 * 1024 * 1024;
  private static Logger logger = LoggerFactory.getLogger(CoreServer.class);

  @Inject
  LogAudioServlet logAudioServlet;

  @Inject
  LogEventServlet logEventServlet;

  @Inject
  LogResponseServlet logResponseServlet;

  @Inject
  StatusServlet statusServlet;

  @Inject
  StreamManager.Factory streamManagerFactory;

  @Inject
  public CoreServer() {}

  public void start(int port) {
    try {
      Server server = new Server();
      HttpConfiguration httpConfig = new HttpConfiguration();
      httpConfig.setSendServerVersion(false);
      HttpConnectionFactory httpFactory = new HttpConnectionFactory(httpConfig);

      ServerConnector connector = new ServerConnector(server, httpFactory);
      connector.setPort(port);
      server.addConnector(connector);

      ServletContextHandler websocketHandler = new ServletContextHandler(
        ServletContextHandler.SESSIONS
      );
      websocketHandler.setErrorHandler(new ApiErrorHandler());
      websocketHandler.setContextPath("/stream");

      WebSocketServerContainerInitializer.configure(
        websocketHandler,
        (servlet, container) -> {
          container.setDefaultMaxBinaryMessageBufferSize(messageSizeLimit);
          container.setDefaultMaxTextMessageBufferSize(messageSizeLimit);
          container.setDefaultMaxSessionIdleTimeout(6 * 3600 * 1000);
          container.addEndpoint(
            ServerEndpointConfig.Builder
              .create(StreamSocket.class, "/")
              .configurator(
                new ServerEndpointConfig.Configurator() {
                  @Override
                  public <T> T getEndpointInstance(Class<T> endpointClass) {
                    try {
                      return endpointClass
                        .getConstructor(StreamManager.Factory.class)
                        .newInstance(streamManagerFactory);
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  }
                }
              )
              .build()
          );
        }
      );

      ServletContextHandler apiHandler = new ServletContextHandler();
      apiHandler.setContextPath("/api");
      apiHandler.addServlet(new ServletHolder(logAudioServlet), "/audio");
      apiHandler.addServlet(new ServletHolder(logEventServlet), "/event");
      apiHandler.addServlet(new ServletHolder(logResponseServlet), "/response");
      apiHandler.addServlet(new ServletHolder(statusServlet), "/status");
      apiHandler.setErrorHandler(new ApiErrorHandler());

      ServletContextHandler rootHandler = new ServletContextHandler();
      rootHandler.setContextPath("/");
      rootHandler.setErrorHandler(new ApiErrorHandler());

      HandlerCollection handlerCollection = new HandlerCollection();
      handlerCollection.setHandlers(new Handler[] { websocketHandler, apiHandler, rootHandler });
      server.setHandler(handlerCollection);

      server.start();
      server.join();
    } catch (Exception e) {
      Logs.logError(logger, "Server exception", e);
    }
  }
}
