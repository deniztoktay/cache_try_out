package tech.pardus.redis.api;

import java.time.Duration;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.redis.dto.StreamMessage;

/** Redis stream bus; every operation applies or refreshes stream retention TTL. */
public interface RedisStreamBus {

  Mono<String> xadd(String streamKey, Map<String, String> fields, Duration retentionTtl);

  /**
   * Pub/sub-style read without a consumer group ({@code XREAD} on the stream). Each caller tracks
   * {@code lastMessageId}; pass {@code null} to receive only messages published after the first
   * read ({@code $}). After each read batch, entries older than {@code streamTtl} are trimmed
   * ({@code XTRIM MINID ~}).
   */
  Flux<StreamMessage> readNoGroup(
      String streamKey,
      String lastMessageId,
      Duration block,
      int count,
      Duration streamTtl);

  /** Blocking poll loop: repeatedly calls {@link #readNoGroup} (for cache projection listeners). */
  Flux<StreamMessage> pollNoGroup(
      String streamKey,
      java.util.function.Supplier<String> lastMessageIdSupplier,
      java.util.function.Consumer<String> lastMessageIdConsumer,
      Duration block,
      int count,
      Duration streamTtl);

  /**
   * Like {@link #pollNoGroup} but stops when {@code continuePolling} returns false (graceful
   * shutdown).
   */
  Flux<StreamMessage> pollNoGroup(
      String streamKey,
      java.util.function.Supplier<String> lastMessageIdSupplier,
      java.util.function.Consumer<String> lastMessageIdConsumer,
      Duration block,
      int count,
      Duration streamTtl,
      java.util.function.BooleanSupplier continuePolling);

  Mono<Void> ensureGroup(String streamKey, String group, Duration streamTtl);

  /**
   * Consumer-group read ({@code XREADGROUP}). After each batch, entries older than {@code streamTtl}
   * are trimmed from the stream.
   */
  Flux<StreamMessage> readGroup(
      String streamKey,
      String group,
      String consumer,
      Duration streamTtl,
      Duration block,
      int count);

  Mono<Void> ack(String streamKey, String group, String messageId, Duration streamTtl);

  Mono<Void> claimStale(
      String streamKey,
      String group,
      String consumer,
      Duration streamTtl,
      Duration minIdleTime);

  Mono<Void> moveToDlq(
      String originalStreamKey,
      String dlqStreamKey,
      String group,
      String messageId,
      Map<String, String> payload,
      String errorReason,
      Duration dlqRetentionTtl,
      Duration originalStreamTtl);
}
