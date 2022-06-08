package core.streaming;

import com.google.protobuf.ByteString;
import core.gen.rpc.EndpointRequest;
import core.gen.rpc.EvaluateRequest;
import core.metadata.EditorStateWithMetadata;
import core.util.CommandLogger;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speechengine.gen.rpc.Alternative;
import speechengine.gen.rpc.AlternativesResponse;
import speechengine.gen.rpc.AudioRequest;
import speechengine.gen.rpc.AudioToAlternativesRequest;
import speechengine.gen.rpc.InitRequest;
import speechengine.gen.rpc.RevertRequest;
import speechengine.gen.rpc.TranscriptsRequest;
import toolbelt.client.ProtobufSocket;
import toolbelt.env.Env;
import toolbelt.logging.Logs;

public class AudioManager {

  // 10 seconds (16000 samples/second, 16-bit samples)
  private final int maximumAudioSize = 16000 * 2 * 10;
  private Logger logger = LoggerFactory.getLogger(AudioManager.class);

  private ByteArrayOutputStream currentCommandAudio = new ByteArrayOutputStream();
  private ByteArrayOutputStream currentChunkAudio = new ByteArrayOutputStream();
  private List<String> currentCommandChunkIds = new ArrayList<>();
  private Optional<EndpointRequest> endpointInProgress = Optional.empty();
  private AlternativesResponse lastAlternativesResponse;
  private List<String> previousCommandChunkIds = new ArrayList<>();
  private ConcurrentLinkedQueue<EvaluateRequest> queue = new ConcurrentLinkedQueue<>();
  private Optional<Session> websocket = Optional.empty();

  private CommandLogger commandLogger;
  private Env env;
  private StreamManager streamManager;

  @AssistedInject
  public AudioManager(CommandLogger commandLogger, Env env, @Assisted StreamManager streamManager) {
    this.commandLogger = commandLogger;
    this.env = env;
    this.streamManager = streamManager;
  }

  @AssistedFactory
  public interface Factory {
    AudioManager create(StreamManager streamManager);
  }

  private void send(AudioToAlternativesRequest request) {
    if (websocket.isEmpty() || !websocket.get().isOpen()) {
      return;
    }

    try {
      websocket.get().getBasicRemote().sendBinary(ByteBuffer.wrap(request.toByteArray()));
    } catch (IOException e) {
      Logs.logError(logger, "AudioManager send error", e);
    }
  }

  public void appendToPrevious() {
    currentCommandChunkIds.addAll(0, previousCommandChunkIds);

    send(
      AudioToAlternativesRequest
        .newBuilder()
        .setRevertRequest(RevertRequest.newBuilder().build())
        .build()
    );

    send(
      AudioToAlternativesRequest
        .newBuilder()
        .setAudioRequest(
          AudioRequest
            .newBuilder()
            .setAudio(ByteString.copyFrom(currentCommandAudio.toByteArray()))
            .build()
        )
        .build()
    );
    // an endpoint request will follow, and it's serialized with this because they're both
    // coming from the client.
  }

  public void connect() {
    if (System.getenv("SERENADE_TEST") != null) {
      return;
    }

    try {
      WebSocketContainer container = ContainerProvider.getWebSocketContainer();
      container.setDefaultMaxSessionIdleTimeout(6 * 3600 * 1000);
      websocket =
        Optional.of(
          container.connectToServer(
            new ProtobufSocket<AlternativesResponse>(
              message -> AlternativesResponse.parseFrom(message),
              response -> processAlternativesResponse(response)
            ),
            URI.create(
              "ws://" +
              System.getenv("SPEECH_ENGINE_HOST") +
              ":" +
              System.getenv("SPEECH_ENGINE_PORT") +
              "/stream/"
            )
          )
        );
    } catch (DeploymentException | IOException e) {
      Logs.logError(logger, "AudioManager connection error", e);
    }
  }

  public void disconnect() {
    if (websocket.isEmpty()) {
      return;
    }

    try {
      websocket.get().close();
    } catch (IOException e) {
      Logs.logError(logger, "AudioManager disconnect error", e);
    } finally {
      websocket = Optional.empty();
    }
  }

  public void processAlternativesResponse(AlternativesResponse response) {
    lastAlternativesResponse = response;
    EndpointRequest endpointRequest = endpointInProgress.get();
    List<String> chunkIds;
    synchronized (this) {
      chunkIds =
        new ArrayList<>(
          currentCommandChunkIds.subList(
            0,
            currentCommandChunkIds.indexOf(endpointRequest.getChunkId()) + 1
          )
        );
    }

    streamManager.processAlternativesResponse(chunkIds, endpointRequest, response);
    endpointInProgress = Optional.empty();
    advanceQueue();
  }

  public void processEndpointRequest(EvaluateRequest request, EditorStateWithMetadata state) {
    String endpointId = request.getEndpointRequest().getEndpointId();
    String chunkId = request.getEndpointRequest().getChunkId();
    boolean finalize = request.getEndpointRequest().getFinalize();
    if (finalize) {
      if (state.getLogAudio()) {
        commandLogger.logAudio(chunkId, state.getToken(), currentChunkAudio.toByteArray(), false);
      }

      currentChunkAudio.reset();
    }

    if (!currentCommandChunkIds.contains(chunkId)) {
      currentCommandChunkIds.add(chunkId);
    }

    queue.add(request);
    advanceQueue();
  }

  public void processAudio(EvaluateRequest request) {
    try {
      currentChunkAudio.write(request.getAudioRequest().getAudio().toByteArray());
    } catch (IOException e) {}

    queue.add(request);
    advanceQueue();
  }

  public void advanceQueue() {
    synchronized (this) {
      if (endpointInProgress.isPresent()) {
        return;
      }

      while (queue.size() > 0) {
        EvaluateRequest request = queue.remove();
        if (request.hasAudioRequest()) {
          ByteString audio = request.getAudioRequest().getAudio();
          if (currentCommandAudio.size() < maximumAudioSize) {
            send(
              AudioToAlternativesRequest
                .newBuilder()
                .setAudioRequest(AudioRequest.newBuilder().setAudio(audio).build())
                .build()
            );
            try {
              currentCommandAudio.write(audio.toByteArray());
            } catch (IOException e) {}
          }
        } else if (
          request.hasEndpointRequest() && queue.stream().noneMatch(e -> e.hasEndpointRequest())
        ) {
          EndpointRequest endpointRequest = request.getEndpointRequest();
          endpointInProgress = Optional.of(endpointRequest);
          if (currentCommandAudio.size() < maximumAudioSize) {
            send(
              AudioToAlternativesRequest
                .newBuilder()
                .setTranscriptsRequest(
                  TranscriptsRequest
                    .newBuilder()
                    .setEndpointId(endpointRequest.getEndpointId())
                    .build()
                )
                .build()
            );
          } else {
            processAlternativesResponse(lastAlternativesResponse);
          }
          break;
        }
      }
    }
  }

  public void resetTranscriptionState() {
    currentCommandChunkIds.clear();
    currentCommandAudio.reset();
    currentChunkAudio.reset();
    endpointInProgress = Optional.empty();
    lastAlternativesResponse = AlternativesResponse.newBuilder().build();
  }

  public void sendInitializeRequest(List<String> phraseHints) {
    previousCommandChunkIds = new ArrayList<>(currentCommandChunkIds);
    resetTranscriptionState();

    InitRequest.Builder init = InitRequest.newBuilder();
    init.addAllHints(phraseHints);
    send(AudioToAlternativesRequest.newBuilder().setInitRequest(init.build()).build());
  }
}
