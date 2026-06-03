package tech.pardus.redis.runtime;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** Closed during application shutdown so reactive Redis work does not run while Lettuce stops. */
@Component
public class RedisRuntimeGate {

  private final AtomicBoolean open = new AtomicBoolean(true);

  public boolean isOpen() {
    return open.get();
  }

  public void close() {
    open.set(false);
  }
}
