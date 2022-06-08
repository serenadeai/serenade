package replayer;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.eclipse.jetty.util.component.LifeCycle;
import speechengine.gen.rpc.AlternativesResponse;
import speechengine.gen.rpc.AudioToAlternativesRequest;
import toolbelt.client.ProtobufSocket;

public class SpeechEngineReplayerClient {

  private String host;

  private CompletableFuture<AlternativesResponse> future;
  private WebSocketContainer container;
  private Optional<Session> websocket = Optional.empty();

  @Inject
  public SpeechEngineReplayerClient() {
    this(System.getenv("SPEECH_ENGINE_HOST") + ":" + System.getenv("SPEECH_ENGINE_PORT"));
  }

  public SpeechEngineReplayerClient(String host) {
    this.host = host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void onResponse(AlternativesResponse response) {
    future.complete(response);
  }

  public void send(AudioToAlternativesRequest request, boolean reset) {
    if (websocket.isEmpty() || !websocket.get().isOpen()) {
      return;
    }

    if (reset) {
      future = new CompletableFuture<>();
    }

    try {
      websocket.get().getBasicRemote().sendBinary(ByteBuffer.wrap(request.toByteArray()));
    } catch (IOException e) {
      System.out.println("SpeechEngineReplayerClient send error" + e);
    }
  }

  public AlternativesResponse await() {
    if (future == null) {
      return null;
    }

    try {
      return future.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public void connect() {
    try {
      container = ContainerProvider.getWebSocketContainer();
      container.setDefaultMaxSessionIdleTimeout(6 * 3600 * 1000);
      websocket =
        Optional.of(
          container.connectToServer(
            new ProtobufSocket<AlternativesResponse>(
              message -> AlternativesResponse.parseFrom(message),
              response -> onResponse(response)
            ),
            URI.create("ws://" + host + "/stream/")
          )
        );
    } catch (DeploymentException | IOException e) {
      System.out.println("SpeechEngineReplayerclient connection error" + e);
      websocket = Optional.empty();
    }
  }

  public void disconnect() {
    if (websocket.isEmpty()) {
      return;
    }

    try {
      websocket.get().close();
      LifeCycle.stop(container);
    } catch (IOException e) {
      System.out.println("SpeechEngineReplayerClient disconnect error" + e);
    }
  }
}
