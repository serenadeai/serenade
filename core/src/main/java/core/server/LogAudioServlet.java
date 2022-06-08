package core.server;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import core.gen.rpc.EmptyResponse;
import core.gen.rpc.LogAudioRequest;
import core.util.CommandLogger;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import toolbelt.server.ApiServlet;

@Singleton
public class LogAudioServlet extends ApiServlet<LogAudioRequest, EmptyResponse> {

  @Inject
  CommandLogger commandLogger;

  @Inject
  public LogAudioServlet() {}

  @Override
  protected LogAudioRequest getRequestFromJson(JsonObject data) {
    return LogAudioRequest.newBuilder().build();
  }

  @Override
  protected LogAudioRequest getRequestFromProtobuf(byte[] data)
    throws InvalidProtocolBufferException {
    return LogAudioRequest.parseFrom(data);
  }

  @Override
  protected EmptyResponse getResponse(LogAudioRequest request, Map<String, String> headers) {
    commandLogger.logAudio(
      request.getChunkId(),
      request.getToken(),
      request.getAudio().toByteArray(),
      true
    );

    return EmptyResponse.newBuilder().build();
  }
}
