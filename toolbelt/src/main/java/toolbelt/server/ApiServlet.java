package toolbelt.server;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolbelt.logging.Logs;

public abstract class ApiServlet<RequestType extends GeneratedMessageV3, ResponseType extends GeneratedMessageV3>
  extends HttpServlet {

  protected Logger logger = LoggerFactory.getLogger(ApiServlet.class);
  protected Gson gson = new Gson();

  protected abstract RequestType getRequestFromProtobuf(byte[] data)
    throws InvalidProtocolBufferException;

  protected abstract RequestType getRequestFromJson(JsonObject data);

  protected abstract ResponseType getResponse(RequestType request, Map<String, String> headers);

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    try {
      Map<String, String> headers = Collections
        .list(request.getHeaderNames())
        .stream()
        .collect(Collectors.toMap(e -> e, request::getHeader));

      boolean protobuf =
        request.getHeader("Content-Type") != null &&
        request.getHeader("Content-Type").equals("application/octet-stream");

      RequestType serviceRequest = protobuf
        ? getRequestFromProtobuf(ByteStreams.toByteArray(request.getInputStream()))
        : getRequestFromJson(
          JsonParser
            .parseString(request.getReader().lines().collect(Collectors.joining()))
            .getAsJsonObject()
        );

      ResponseType serviceResponse = getResponse(serviceRequest, headers);
      response.setStatus(HttpServletResponse.SC_OK);
      if (protobuf) {
        response.setContentType("application/octet-stream");
        serviceResponse.writeTo(response.getOutputStream());
      } else {
        response.setContentType("application/json");
        response.getWriter().println(JsonFormat.printer().print(serviceResponse));
      }
    } catch (Exception e) {
      Logs.logError(logger, "API Servlet exception", e);
      throw new RuntimeException(e);
    }
  }

  protected boolean getAsBoolean(JsonObject data, String field) {
    JsonElement result = data.get(field);
    return result == null ? false : result.getAsBoolean();
  }

  protected int getAsInt(JsonObject data, String field) {
    JsonElement result = data.get(field);
    return result == null ? 0 : result.getAsInt();
  }

  protected String getAsString(JsonObject data, String field) {
    JsonElement result = data.get(field);
    return result == null ? "" : result.getAsString();
  }

  protected String truncateText(String text) {
    return text.substring(0, Math.min(text.length(), 1000));
  }
}
