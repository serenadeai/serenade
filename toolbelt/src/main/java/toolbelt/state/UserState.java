package toolbelt.state;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import toolbelt.logging.Logs;

public class UserState {

  private boolean memory = false;
  private Logger logger = LoggerFactory.getLogger(UserState.class);
  private JedisPool db;
  private static Map<String, List<String>> inMemoryState = new HashMap<>();

  @Inject
  public UserState() {
    this.memory = System.getenv("IN_MEMORY") != null;

    if (!this.memory) {
      this.db = RedisFactory.get();
    }
  }

  private String key(String name, String token) {
    return name + ":" + token;
  }

  public void addToEndOfList(String name, String token, String value) {
    if (memory) {
      List<String> values = inMemoryState.get(key(name, token));
      values.add(value);
      inMemoryState.put(key(name, token), values);
      return;
    }

    try (Jedis resource = db.getResource()) {
      resource.rpush(key(name, token), value);
    }
  }

  public void addToStartOfList(String name, String token, String value) {
    if (memory) {
      List<String> values = inMemoryState.get(key(name, token));
      if (values == null) {
        values = new ArrayList<>();
      }

      values.add(0, value);
      inMemoryState.put(key(name, token), values);
      return;
    }

    try (Jedis resource = db.getResource()) {
      resource.lpush(key(name, token), value);
    }
  }

  public Optional<String> get(String name, String token) {
    if (memory) {
      List<String> result = inMemoryState.get(key(name, token));
      return result == null || result.size() == 0 ? Optional.empty() : Optional.of(result.get(0));
    }

    try (Jedis resource = db.getResource()) {
      String result = resource.get(key(name, token));
      return result == null ? Optional.empty() : Optional.of(result);
    }
  }

  public List<String> getList(String name, String token) {
    if (memory) {
      List<String> result = inMemoryState.get(key(name, token));
      return result == null ? new ArrayList<>() : result;
    }

    try (Jedis resource = db.getResource()) {
      return resource.lrange(key(name, token), 0, -1);
    }
  }

  public <T extends Message> Optional<T> getProto(String name, String token, T.Builder builder) {
    Optional<String> serialized = get(name, token);
    if (serialized.isEmpty() || serialized.get().isEmpty()) {
      return Optional.empty();
    }

    T result = stringToProto(serialized.get(), builder);
    return result == null ? Optional.empty() : Optional.of(result);
  }

  public String protoToString(MessageOrBuilder value) {
    try {
      return JsonFormat.printer().print(value);
    } catch (InvalidProtocolBufferException e) {
      Logs.logError(logger, "Protobuf encoding error", e);
    }

    return "";
  }

  public void remove(String name, String token) {
    if (memory) {
      inMemoryState.remove(key(name, token));
      return;
    }

    try (Jedis resource = db.getResource()) {
      resource.del(key(name, token));
    }
  }

  public void set(String name, String token, String value) {
    if (memory) {
      inMemoryState.put(key(name, token), new ArrayList<>(Arrays.asList(value)));
      return;
    }

    try (Jedis resource = db.getResource()) {
      resource.set(key(name, token), value);
    }
  }

  public void setProto(String name, String token, MessageOrBuilder value) {
    set(name, token, protoToString(value));
  }

  @SuppressWarnings("unchecked")
  public <T extends Message> T stringToProto(String value, T.Builder builder) {
    try {
      JsonFormat.parser().merge(value, builder);
      return (T) builder.build();
    } catch (InvalidProtocolBufferException e) {
      Logs.logError(logger, "Protobuf encoding error", e);
    }

    return null;
  }

  public void trimList(String name, String token, int limit) {
    if (memory) {
      List<String> result = inMemoryState.get(key(name, token));
      if (result == null) {
        return;
      }

      if (result.size() > limit) {
        result.subList(limit, result.size()).clear();
        inMemoryState.put(key(name, token), result);
      }

      return;
    }

    try (Jedis resource = db.getResource()) {
      resource.ltrim(key(name, token), 0, limit - 1);
    }
  }
}
