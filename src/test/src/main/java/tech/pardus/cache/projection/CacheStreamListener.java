package tech.pardus.cache.projection;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.redis.api.RedisStreamBus;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.runtime.RedisRuntimeGate;

@Slf4j
public class CacheStreamListener implements SmartLifecycle {

  private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

  private final RedisStreamBus streamBus;
  private final CacheEntityDescriptor descriptor;
  private final L1ProjectionHandler projectionHandler;
  private final RedisRuntimeGate redisGate;
  private final String streamKey;
  private final AtomicReference<String> lastMessageId = new AtomicReference<>();
  private final AtomicReference<Disposable> subscription = new AtomicReference<>();
  private final AtomicBoolean running = new AtomicBoolean(false);

  public CacheStreamListener(
      RedisStreamBus streamBus,
      CacheEntityDescriptor descriptor,
      L1ProjectionHandler projectionHandler,
      RedisRuntimeGate redisGate) {
    this.streamBus = streamBus;
    this.descriptor = descriptor;
    this.projectionHandler = projectionHandler;
    this.redisGate = redisGate;
    this.streamKey = CacheKeyLayout.changeStreamKey(descriptor.namespace());
  }

  @Override
  public void start() {
    var streamConfig = descriptor.streamConfig().orElseThrow();
    if (!streamConfig.isListenerEnabled()) {
      log.info("Cache stream listener disabled for {}", descriptor.entityName());
      return;
    }
    if (!running.compareAndSet(false, true)) {
      return;
    }
    log.info("Starting cache stream listener for {} on {}", descriptor.entityName(), streamKey);
    Disposable disposable =
        streamBus
            .pollNoGroup(
                streamKey,
                lastMessageId::get,
                lastMessageId::set,
                streamConfig.getPollBlock(),
                streamConfig.getBatchSize(),
                streamConfig.getRetentionTtl(),
                this::shouldPoll)
            .concatMap(
                msg ->
                    Mono.fromRunnable(
                        () -> projectionHandler.onChange(CacheChangeStreamMessage.from(msg))))
            .doOnError(
                ex -> {
                  if (shouldPoll()) {
                    log.error(
                        "Cache stream listener error for {} on {}",
                        descriptor.entityName(),
                        streamKey,
                        ex);
                  }
                })
            .retryWhen(
                Retry.backoff(Long.MAX_VALUE, RETRY_DELAY)
                    .maxBackoff(Duration.ofSeconds(30))
                    .filter(ex -> shouldPoll()))
            .onErrorComplete()
            .subscribe();
    subscription.set(disposable);
  }

  @Override
  public void stop() {
    if (!running.compareAndSet(true, false)) {
      return;
    }
    var disposable = subscription.getAndSet(null);
    if (disposable != null) {
      disposable.dispose();
      log.info("Stopped cache stream listener for {}", descriptor.entityName());
    }
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  private boolean shouldPoll() {
    return running.get() && redisGate.isOpen();
  }

  /**
   * Stop stream polling early on shutdown: higher phase value stops before Lettuce (phase 0).
   */
  @Override
  public int getPhase() {
    return Ordered.LOWEST_PRECEDENCE - 100;
  }
}
