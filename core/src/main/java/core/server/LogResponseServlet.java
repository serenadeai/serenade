package core.server;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import core.gen.rpc.EmptyResponse;
import core.gen.rpc.LogResponseRequest;
import core.metadata.EditorStateWithMetadata;
import core.util.CommandLogger;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import toolbelt.server.ApiServlet;

@Singleton
public class LogResponseServlet extends ApiServlet<LogResponseRequest, EmptyResponse> {

  @Inject
  CommandLogger commandLogger;

  @Inject
  public LogResponseServlet() {}

  @Override
  protected LogResponseRequest getRequestFromJson(JsonObject data) {
    return LogResponseRequest.newBuilder().build();
  }

  @Override
  protected LogResponseRequest getRequestFromProtobuf(byte[] data)
    throws InvalidProtocolBufferException {
    return LogResponseRequest.parseFrom(data);
  }

  @Override
  protected EmptyResponse getResponse(LogResponseRequest request, Map<String, String> headers) {
    commandLogger.logResponse(
      request.getResponse(),
      new EditorStateWithMetadata(request.getEditorState())
    );

    return EmptyResponse.newBuilder().build();
  }
}
