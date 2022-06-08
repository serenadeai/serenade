package core.server;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import core.gen.rpc.EmptyResponse;
import core.gen.rpc.LogEventRequest;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import toolbelt.logging.Logs;
import toolbelt.server.ApiServlet;

@Singleton
public class LogEventServlet extends ApiServlet<LogEventRequest, EmptyResponse> {

  private Logger logger = LoggerFactory.getLogger(LogEventServlet.class);

  @Inject
  public LogEventServlet() {}

  @Override
  protected LogEventRequest getRequestFromJson(JsonObject data) {
    return LogEventRequest
      .newBuilder()
      .setLog(getAsString(data, "log"))
      .setToken(getAsString(data, "token"))
      .setClientIdentifier(getAsString(data, "clientIdentifier"))
      .setEvent(getAsString(data, "event"))
      .setEvent(getAsString(data, "data"))
      .setDt(getAsInt(data, "dt"))
      .build();
  }

  @Override
  protected LogEventRequest getRequestFromProtobuf(byte[] data)
    throws InvalidProtocolBufferException {
    return LogEventRequest.parseFrom(data);
  }

  @Override
  protected EmptyResponse getResponse(LogEventRequest request, Map<String, String> headers) {
    Map<String, Object> data = new HashMap<>();
    if (!request.getToken().equals("")) {
      data.put("token", request.getToken());
    }
    if (!request.getClientIdentifier().equals("")) {
      data.put("client_identifier", Logs.jsonToObject(request.getClientIdentifier()));
    }
    if (!request.getEvent().equals("")) {
      data.put("event", request.getEvent());
    }
    if (!request.getData().equals("")) {
      data.put("data", Logs.jsonToObject(request.getData()));
    }
    if (request.getDt() > 0) {
      data.put("dt", request.getDt());
    }

    Logs.logData(logger, Level.TRACE, request.getLog(), data);
    return EmptyResponse.newBuilder().build();
  }
}
