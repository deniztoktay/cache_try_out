package tech.pardus.redis.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Closes {@link RedisRuntimeGate} before Lettuce stops so background polls/retries do not log
 * "LettuceConnectionFactory is stopping" indefinitely.
 */
@Slf4j
@Component
public class RedisShutdownLifecycle implements SmartLifecycle {

  private final RedisRuntimeGate gate;
  private volatile boolean running;

  public RedisShutdownLifecycle(RedisRuntimeGate gate) {
    this.gate = gate;
  }

  @Override
  public void start() {
    running = true;
  }

  @Override
  public void stop() {
    if (!running) {
      return;
    }
    running = false;
    gate.close();
    log.info("Redis runtime gate closed; background Redis work will not start");
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  /** Stop before stream listeners and well before Lettuce (phase 0). */
  @Override
  public int getPhase() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
