package toolbelt.state;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisFactory {

  private static JedisPool db;

  public static JedisPool get() {
    if (db != null) {
      return db;
    }

    String redis = System.getenv("REDIS");
    db = new JedisPool(new JedisPoolConfig(), redis);
    return db;
  }
}
