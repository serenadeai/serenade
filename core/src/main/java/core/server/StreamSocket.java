package core.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.gen.rpc.EvaluateRequest;
import core.streaming.StreamManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
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

@ServerEndpoint(value = "/")
public class StreamSocket {

  private Logger logger = LoggerFactory.getLogger(StreamSocket.class);
  private StreamManager.Factory streamManagerFactory;
  private StreamManager manager;

  public StreamSocket(StreamManager.Factory streamManagerFactory) {
    this.streamManagerFactory = streamManagerFactory;
  }

  @OnOpen
  public void onWebSocketConnect(Session session) {
    manager = streamManagerFactory.create(session);
    manager.connect();
  }

  @OnMessage
  public void onWebSocketMessage(Session session, byte[] message) throws IOException {
    manager.onMessage(EvaluateRequest.parseFrom(message));
  }

  @OnClose
  public void onWebSocketClose(Session session) {
    manager.disconnect();
  }

  @OnError
  public void onWebSocketError(Throwable e) {
    Logs.logError(logger, "StreamSocket WebSocket error", e);
    manager.disconnect();
  }
}
