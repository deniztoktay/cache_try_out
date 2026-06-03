package tech.pardus.redis.support;

import io.lettuce.core.RedisCommandExecutionException;
import org.springframework.data.redis.RedisSystemException;

public final class RedisErrors {

  private RedisErrors() {}

  public static boolean isNoSuchKey(Throwable ex) {
    var current = ex;
    while (current != null) {
      if (current instanceof RedisCommandExecutionException rce) {
        var msg = rce.getMessage();
        if (msg != null && msg.contains("no such key")) {
          return true;
        }
      }
      if (current instanceof RedisSystemException rse) {
        var msg = rse.getMessage();
        if (msg != null && msg.contains("no such key")) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }
}
