package core.streaming;

import core.evaluator.CallbackEvaluator;
import core.evaluator.ParsedTranscript;
import core.evaluator.TranscriptEvaluator;
import core.evaluator.TranscriptParser;
import core.gen.rpc.AuthenticateResponse;
import core.gen.rpc.CallbackRequest;
import core.gen.rpc.CallbackType;
import core.gen.rpc.Command;
import core.gen.rpc.CommandType;
import core.gen.rpc.CommandsResponse;
import core.gen.rpc.CommandsResponseAlternative;
import core.gen.rpc.EditorState;
import core.gen.rpc.EndpointRequest;
import core.gen.rpc.EvaluateAudioRequest;
import core.gen.rpc.EvaluateRequest;
import core.gen.rpc.EvaluateResponse;
import core.gen.rpc.KeepAliveResponse;
import core.gen.rpc.Language;
import core.metadata.EditorStateWithMetadata;
import core.util.CommandLogger;
import core.util.PhraseHintExtractor;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speechengine.gen.rpc.Alternative;
import speechengine.gen.rpc.AlternativesResponse;
import speechengine.gen.rpc.AudioRequest;
import speechengine.gen.rpc.AudioToAlternativesRequest;
import toolbelt.client.ServiceHttpClient;
import toolbelt.logging.Logs;

public class StreamManager {

  private boolean appendToPreviousInProgress = false;
  private Logger logger = LoggerFactory.getLogger(StreamManager.class);
  private CommandsResponse cachedResponse = CommandsResponse.newBuilder().build();
  private List<ParsedTranscript> cachedTranscripts = new ArrayList<>();
  private boolean computedResponseForEditorState = false;
  private EditorStateWithMetadata editorState = new EditorStateWithMetadata();
  private EditorStateWithMetadata editorStateAtStartOfCurrentCommand = new EditorStateWithMetadata();
  private EditorStateWithMetadata editorStateAtStartOfPreviousCommand = new EditorStateWithMetadata();
  private CompletionStage<Void> processAlternativesResponse = CompletableFuture.completedFuture(
    null
  );

  private AudioManager audioManager;
  private CallbackEvaluator callbackEvaluator;
  private CommandLogger commandLogger;
  private PhraseHintExtractor phraseHintExtractor;
  private ServiceHttpClient serviceHttpClient;
  private SilenceDeterminer silenceDeterminer;
  private TranscriptEvaluator transcriptEvaluator;
  private TranscriptParser transcriptParser;
  private Optional<Session> websocket = Optional.empty();

  @AssistedInject
  public StreamManager(
    AudioManager.Factory audioManagerFactory,
    CommandLogger commandLogger,
    TranscriptParser transcriptParser,
    TranscriptEvaluator transcriptEvaluator,
    PhraseHintExtractor phraseHintExtractor,
    CallbackEvaluator callbackEvaluator,
    SilenceDeterminer silenceDeterminer,
    ServiceHttpClient serviceHttpClient,
    @Assisted Session websocket
  ) {
    this.audioManager = audioManagerFactory.create(this);
    this.commandLogger = commandLogger;
    this.transcriptParser = transcriptParser;
    this.transcriptEvaluator = transcriptEvaluator;
    this.phraseHintExtractor = phraseHintExtractor;
    this.callbackEvaluator = callbackEvaluator;
    this.silenceDeterminer = silenceDeterminer;
    this.serviceHttpClient = serviceHttpClient;
    // only null when we're instantiating to warm singletons
    this.websocket = Optional.ofNullable(websocket);

    resetTranscriptionState();
  }

  @AssistedFactory
  public interface Factory {
    StreamManager create(Session websocket);
  }

  private String getClientIdentifier() {
    if (editorState != null && editorState.getClientIdentifier().length() > 0) {
      return editorState.getClientIdentifier();
    }

    return "";
  }

  private void handleException(String message, Throwable e) {
    Logs.logError(logger, message, e);
    disconnect();
  }

  private boolean shouldUseCachedResponse(List<ParsedTranscript> transcripts) {
    if (!computedResponseForEditorState) {
      return false;
    }

    boolean result = true;
    if (transcripts.size() == cachedTranscripts.size()) {
      for (int i = 0; i < transcripts.size(); i++) {
        if (!transcripts.get(i).transcript().equals(cachedTranscripts.get(i).transcript())) {
          result = false;
          break;
        }
      }
    } else {
      result = false;
    }

    return result;
  }

  private void resetTranscriptionState() {
    appendToPreviousInProgress = false;
    cachedResponse = CommandsResponse.newBuilder().build();
    cachedTranscripts.clear();
    computedResponseForEditorState = false;
    audioManager.resetTranscriptionState();
  }

  private void sendCommandsResponse(
    List<ParsedTranscript> transcripts,
    Optional<EndpointRequest> endpointRequest,
    List<String> chunkIds,
    boolean includeAlternatives
  ) {
    boolean text = !endpointRequest.isPresent();
    boolean useCachedResponse = shouldUseCachedResponse(transcripts);
    String endpointId = endpointRequest
      .map(r -> r.getEndpointId())
      .orElse(UUID.randomUUID().toString());
    boolean finalize = endpointRequest.map(r -> r.getFinalize()).orElse(true);

    CommandsResponse response = CommandsResponse.newBuilder().build();
    if (!useCachedResponse) {
      // Check if we're a non-noise transcript and evaluate. We still send down empty responses
      // because the client always expects some sort of response for each request, even if it's
      // empty.
      if (transcripts.size() != 0) {
        response =
          Logs.logTime(
            logger,
            "core.evaluate-transcript",
            Map.of("endpoint_id", endpointId),
            () -> transcriptEvaluator.evaluate(transcripts, editorState, finalize)
          );
      }
    }

    response =
      CommandsResponse
        .newBuilder(useCachedResponse ? cachedResponse : response)
        .setEndpointId(endpointId)
        .setChunkId(chunkIds.size() > 0 ? chunkIds.get(chunkIds.size() - 1) : "")
        .setTextResponse(text)
        .setSilenceThreshold(silenceDeterminer.threshold(transcripts, getClientIdentifier()))
        .setFinal(finalize)
        .clearChunkIds()
        .addAllChunkIds(chunkIds)
        .build();

    if (finalize) {
      commandLogger.logResponse(response, editorState);
    }

    Logs.stopTimerAndLog(
      "endpoint:" + endpointId,
      logger,
      "core.endpoint",
      Map.of("endpoint_id", endpointId)
    );

    cachedResponse = response;
    if (!includeAlternatives) {
      response = CommandsResponse.newBuilder(response).clearAlternatives().build();
    }

    send(EvaluateResponse.newBuilder().setCommandsResponse(response).build());
  }

  private void updateEditorState(EditorState newEditorState) {
    // if LANGUAGE_NONE is sent (e.g., from a test that forgot to set the language)
    // then use DEFAULT instead
    if (newEditorState.getLanguage().equals(Language.LANGUAGE_NONE)) {
      newEditorState =
        EditorState.newBuilder(newEditorState).setLanguage(Language.LANGUAGE_DEFAULT).build();
    }

    EditorStateWithMetadata newEditorStateWithMetadata = new EditorStateWithMetadata(
      newEditorState
    );

    if (
      !newEditorStateWithMetadata.getSource().equals(editorState.getSource()) ||
      newEditorStateWithMetadata.getCursor() != editorState.getCursor()
    ) {
      computedResponseForEditorState = false;
    }

    editorState = newEditorStateWithMetadata;
  }

  public void connect() {
    audioManager.connect();
  }

  public void disconnect() {
    audioManager.disconnect();
    if (websocket.isEmpty()) {
      return;
    }

    try {
      websocket.get().close();
    } catch (IOException e) {
      Logs.logError(logger, "StreamManager disconnect error", e);
    } finally {
      websocket = Optional.empty();
    }
  }

  public void onMessage(EvaluateRequest request) {
    // maintain backwards compatibility with an old callback mechanism
    if (request.hasTextRequest() && request.getTextRequest().getText().equals("callback open")) {
      request =
        EvaluateRequest
          .newBuilder()
          .setCallbackRequest(
            CallbackRequest.newBuilder().setType(CallbackType.CALLBACK_TYPE_OPEN_FILE)
          )
          .build();
    }

    if (request.hasAudioRequest()) {
      audioManager.processAudio(request);
    } else if (request.hasTextRequest()) {
      List<String> transcripts = Arrays.asList(request.getTextRequest().getText().split(";"));
      try {
        sendCommandsResponse(
          transcriptParser.parse(
            transcripts
              .stream()
              .map(e -> Alternative.newBuilder().setTranscript(e).build())
              .collect(Collectors.toList()),
            editorState,
            request.getTextRequest().getRerank()
          ),
          Optional.empty(),
          Arrays.asList(),
          request.getTextRequest().getIncludeAlternatives()
        );
      } catch (Exception e) {
        handleException("Uncaught text request exception", e);
      }
    } else if (request.hasInitializeRequest()) {
      if (!appendToPreviousInProgress) {
        editorStateAtStartOfPreviousCommand = editorStateAtStartOfCurrentCommand;
      }

      resetTranscriptionState();
      updateEditorState(request.getInitializeRequest().getEditorState());
      editorStateAtStartOfCurrentCommand = editorState;
      audioManager.sendInitializeRequest(
        phraseHintExtractor.extract(editorState.getSource(), editorState.getCustomHints())
      );
    } else if (request.hasDisableRequest()) {
      disconnect();
    } else if (request.hasEditorStateRequest()) {
      if (!appendToPreviousInProgress) {
        updateEditorState(request.getEditorStateRequest().getEditorState());
      }
    } else if (request.hasAppendToPreviousRequest()) {
      appendToPreviousInProgress = true;
      audioManager.appendToPrevious();
      editorState = editorStateAtStartOfPreviousCommand;
    } else if (request.hasEndpointRequest()) {
      Logs.startTimer("endpoint:" + request.getEndpointRequest().getEndpointId());
      audioManager.processEndpointRequest(request, editorState);
    } else if (request.hasCallbackRequest()) {
      Optional<CommandsResponse> response = callbackEvaluator.evaluate(
        request.getCallbackRequest(),
        editorState
      );
      if (response.isPresent()) {
        send(EvaluateResponse.newBuilder().setCommandsResponse(response.get()).build());
      }
    } else if (request.hasKeepAliveRequest()) {
      send(
        EvaluateResponse
          .newBuilder()
          .setKeepAliveResponse(
            KeepAliveResponse.newBuilder().setDt(System.currentTimeMillis()).build()
          )
          .build()
      );
    } else if (request.hasAuthenticateRequest()) {
      // remove once 1.10 is deprecated
      send(
        EvaluateResponse
          .newBuilder()
          .setAuthenticateResponse(AuthenticateResponse.newBuilder().setSuccess(true).build())
          .build()
      );
    }
  }

  public void processAlternativesResponse(
    List<String> chunkIds,
    EndpointRequest endpointRequest,
    AlternativesResponse alternativesResponse
  ) {
    processAlternativesResponse =
      processAlternativesResponse
        .thenAcceptAsync(
          v -> {
            List<ParsedTranscript> transcripts = transcriptParser.parse(
              alternativesResponse.getAlternativesList(),
              editorState,
              true
            );
            sendCommandsResponse(transcripts, Optional.of(endpointRequest), chunkIds, true);
            computedResponseForEditorState = true;
            cachedTranscripts =
              transcripts.stream().map(e -> new ParsedTranscript(e)).collect(Collectors.toList());
          }
        )
        .exceptionally(
          e -> {
            handleException("Uncaught process alternative exception", e);
            return null;
          }
        );
  }

  public void send(EvaluateResponse response) {
    if (websocket.isEmpty() || !websocket.get().isOpen()) {
      return;
    }

    try {
      websocket.get().getBasicRemote().sendBinary(ByteBuffer.wrap(response.toByteArray()));
    } catch (IOException e) {
      Logs.logError(logger, "StreamManager send error", e);
    }
  }
}
