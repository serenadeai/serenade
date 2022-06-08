package toolbelt.client;

import core.gen.rpc.EvaluateRequest;
import core.gen.rpc.EvaluateResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolbelt.logging.Logs;

public class CoreClient {

  private Logger logger = LoggerFactory.getLogger(CoreClient.class);
  private String host;

  private CompletableFuture<EvaluateResponse> future;
  private WebSocketContainer container;
  private Session websocket;

  public CoreClient() {
    this(System.getenv("CORE_HOST") + ":" + System.getenv("CORE_PORT"));
  }

  public CoreClient(String host) {
    this.host = host;
  }

  private void onResponse(EvaluateResponse response) {
    future.complete(response);
  }

  public EvaluateResponse await() {
    if (future == null) {
      return null;
    }

    try {
      return future.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      return null;
    }
  }

  public void connect() {
    try {
      container = ContainerProvider.getWebSocketContainer();
      container.setDefaultMaxSessionIdleTimeout(6 * 3600 * 1000);
      websocket =
        container.connectToServer(
          new ProtobufSocket<EvaluateResponse>(
            message -> EvaluateResponse.parseFrom(message),
            response -> onResponse(response)
          ),
          URI.create("ws://" + host + "/stream/")
        );
    } catch (DeploymentException | IOException e) {
      Logs.logError(logger, "Core client connection error", e);
    }
  }

  public void disconnect() {
    if (websocket == null) {
      return;
    }

    try {
      websocket.close();
      LifeCycle.stop(container);
    } catch (IOException e) {
      Logs.logError(logger, "CoreClient disconnect error", e);
    }
  }

  public void send(EvaluateRequest request, boolean reset) {
    if (reset) {
      future = new CompletableFuture<>();
    }

    try {
      websocket.getBasicRemote().sendBinary(ByteBuffer.wrap(request.toByteArray()));
    } catch (Exception e) {
      Logs.logError(logger, "CoreClient send error", e);
    }
  }

  public void send(EvaluateRequest request) {
    send(request, true);
  }
}
