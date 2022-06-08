package toolbelt.logging;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import toolbelt.env.Env;

public class Logs {

  private static Env env = new Env();
  private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  private static Random random = new Random();
  private static String timerPostfix = ".timer";

  private static Cache<String, Long> timers = CacheBuilder
    .newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(1, TimeUnit.MINUTES)
    .build();

  private static void log(Logger logger, Level level, String name, Map<String, Object> data) {
    HashMap<String, Object> map = new HashMap<>();
    map.putAll(data);

    map.put("container", System.getenv("CONTAINER_ID"));
    map.put("dt", System.currentTimeMillis());
    map.put("env", System.getenv("ENV"));
    map.put("git_commit", System.getenv("GIT_COMMIT"));
    map.put("log_id", UUID.randomUUID().toString());
    map.put("log_name", name);
    map.put("service_name", System.getenv("SERVICE"));

    try {
      String message = gson.toJson(map);
      if (level == Level.DEBUG) {
        logger.debug(message);
      } else if (level == Level.ERROR) {
        logger.error(message);
      } else if (level == Level.INFO) {
        logger.info(message);
      } else if (level == Level.TRACE) {
        logger.trace(message);
      } else if (level == Level.WARN) {
        logger.warn(message);
      }
    } catch (Exception e) {}
  }

  private static void logTime(Logger logger, String name, Map<String, Object> data, long duration) {
    // sample production timer logs, since there are so many
    if (env.docker() || env.local() || random.nextDouble() > 0.02) {
      return;
    }

    Map<String, Object> map = new HashMap<>(data);
    map.put("duration", duration);
    log(logger, Level.TRACE, name + timerPostfix, map);
  }

  public static void logData(Logger logger, String name, Map<String, Object> data) {
    log(logger, Level.DEBUG, name, data);
  }

  public static void logData(Logger logger, Level level, String name, Map<String, Object> data) {
    log(logger, level, name, data);
  }

  public static void logError(Logger logger, String message, Throwable t) {
    logger.error(message, t);
  }

  public static void logInfo(Logger logger, String name, String message) {
    log(logger, Level.INFO, name, Map.of("message", message));
  }

  public static <T> T logTime(
    Logger logger,
    String name,
    Map<String, Object> data,
    Supplier<T> operation
  ) {
    long start = System.currentTimeMillis();
    T result = operation.get();
    long duration = System.currentTimeMillis() - start;

    logTime(logger, name, data, duration);
    return result;
  }

  public static void logTime(
    Logger logger,
    String name,
    Map<String, Object> data,
    Runnable operation
  ) {
    logTime(
      logger,
      name,
      data,
      () -> {
        operation.run();
        return null;
      }
    );
  }

  public static Object jsonToObject(String identifier) {
    Object result = gson.fromJson(identifier, Object.class);
    if (result == null) {
      result = new String("");
    }

    return result;
  }

  public static void startTimer(String timerId) {
    timers.put(timerId, System.currentTimeMillis());
  }

  public static void stopTimerAndLog(
    String timerId,
    Logger logger,
    String name,
    Map<String, Object> data
  ) {
    Long start = timers.getIfPresent(timerId);
    if (start == null) {
      return;
    }

    logTime(logger, name, data, System.currentTimeMillis() - start);
    timers.invalidate(timerId);
  }

  public static Optional<String> versionFromClientIdentifier(String clientIdentifier) {
    try {
      JsonObject parsed = JsonParser.parseString(clientIdentifier).getAsJsonObject();
      if (parsed.get("version") == null) {
        return Optional.empty();
      }

      String version = parsed.get("version").getAsString();
      if (version.equals("")) {
        return Optional.empty();
      }

      return Optional.of(version);
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
