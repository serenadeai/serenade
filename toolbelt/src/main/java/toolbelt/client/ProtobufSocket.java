package toolbelt.client;

import com.google.protobuf.GeneratedMessageV3;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolbelt.logging.Logs;

@ClientEndpoint
@ServerEndpoint(value = "/")
public class ProtobufSocket<T extends GeneratedMessageV3> {

  @FunctionalInterface
  public interface Parser<V extends GeneratedMessageV3> {
    V apply(byte[] message) throws IOException;
  }

  private Logger logger = LoggerFactory.getLogger(ProtobufSocket.class);
  private Parser<T> parse;
  private Consumer<T> onMessage;

  public ProtobufSocket(Parser<T> parse, Consumer<T> onMessage) {
    this.parse = parse;
    this.onMessage = onMessage;
  }

  @OnOpen
  public void onWebSocketConnect(Session session) {}

  @OnMessage
  public void onWebSocketMessage(Session session, byte[] message) {
    try {
      this.onMessage.accept(this.parse.apply(message));
    } catch (IOException e) {
      Logs.logError(logger, "WebSocket encoding error", e);
    }
  }

  @OnClose
  public void onWebSocketClose(Session session) {}

  @OnError
  public void onWebSocketError(Throwable e) {
    Logs.logError(logger, "WebSocket error", e);
  }
}
