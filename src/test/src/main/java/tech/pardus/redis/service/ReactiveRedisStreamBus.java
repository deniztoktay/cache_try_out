package tech.pardus.redis.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.redis.api.RedisStreamBus;
import tech.pardus.redis.dto.StreamMessage;
import tech.pardus.redis.runtime.RedisRuntimeGate;
import tech.pardus.redis.support.TtlPolicy;

@Slf4j
public class ReactiveRedisStreamBus implements RedisStreamBus {
  private static final RedisScript<Long> TRIM_MINID_SCRIPT =
      RedisScript.of("return redis.call('XTRIM', KEYS[1], 'MINID', '~', ARGV[1])", Long.class);

  private static final Duration DEFAULT_TRIM_THROTTLE = Duration.ofSeconds(5);

  private final ReactiveStringRedisTemplate redisTemplate;
  private final ReactiveStreamOperations<String, String, String> streamOps;
  private final ConcurrentHashMap<String, AtomicLong> lastTrimEpochMsByStream =
      new ConcurrentHashMap<>();
  private final RedisRuntimeGate redisGate;

  public ReactiveRedisStreamBus(ReactiveStringRedisTemplate redis, RedisRuntimeGate redisGate) {
    this.redisTemplate = redis;
    this.streamOps = redis.opsForStream();
    this.redisGate = redisGate;
  }

  @Override
  public Mono<String> xadd(String streamKey, Map<String, String> fields, Duration retentionTtl) {
    var ttl = TtlPolicy.requirePositive(retentionTtl, "xadd");
    var record = StreamRecords.newRecord().in(streamKey).ofMap(fields);

    return streamOps
        .add(record)
        .flatMap(
            id -> {
              if (Objects.isNull(id)) {
                return Mono.error(
                    wrapStreamError(
                        "XADD_RETURNED_NULL_ID",
                        streamKey,
                        new IllegalStateException("XADD returned null RecordId")));
              }
              return Mono.just(id.getValue());
            })
        .flatMap(id -> touchStreamTtl(streamKey, ttl).thenReturn(id))
        .doOnSuccess(
            id -> {
              log.debug("Published to stream: {}, messageId: {}", streamKey, id);
              trimByAgeAsync(streamKey, ttl, DEFAULT_TRIM_THROTTLE);
            })
        .onErrorMap(ex -> wrapStreamError("XADD_FAILED", streamKey, ex));
  }

  /** Drops stream entries older than {@code retentionTtl} (Redis {@code XTRIM MINID ~}). */
  private Mono<Long> trimExpired(String streamKey, Duration retentionTtl) {
    var minId = Instant.now().minus(retentionTtl).toEpochMilli() + "-0";
    return redisTemplate
        .execute(TRIM_MINID_SCRIPT, List.of(streamKey), List.of(minId))
        .next()
        .defaultIfEmpty(0L)
        .doOnNext(
            trimmed ->
                log.debug(
                    "Trimmed {} expired entries from stream {} (MINID ~ {})", trimmed, streamKey, minId));
  }

  private void trimByAgeAsync(String streamKey, Duration retentionTtl, Duration throttle) {
    if (!redisGate.isOpen()) {
      return;
    }
    var nowMs = System.currentTimeMillis();
    var lastTrimRef = lastTrimEpochMsByStream.computeIfAbsent(streamKey, k -> new AtomicLong(0));
    var lastMs = lastTrimRef.get();
    if (nowMs - lastMs < throttle.toMillis()) {
      return;
    }
    if (!lastTrimRef.compareAndSet(lastMs, nowMs)) {
      return;
    }
    trimExpired(streamKey, retentionTtl)
        .subscribe(
            trimmedCount -> {},
            ex -> log.warn("Background stream trimming failed for stream {}", streamKey, ex));
  }

  private <T> Flux<T> withTrimAfterRead(Flux<T> readFlux, String streamKey, Duration streamTtl) {
    return readFlux.concatWith(trimExpired(streamKey, streamTtl).thenMany(Flux.empty()));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Flux<StreamMessage> readNoGroup(
      String streamKey,
      String lastMessageId,
      Duration block,
      int count,
      Duration streamTtl) {

    TtlPolicy.requirePositive(streamTtl, "readNoGroup");
    var waitTime = Objects.nonNull(block) ? block : Duration.ofSeconds(1);
    var options = StreamReadOptions.empty().block(waitTime).count(Math.max(1, count));
    var offset =
        Objects.isNull(lastMessageId) || lastMessageId.isBlank()
            ? ReadOffset.latest()
            : ReadOffset.from(lastMessageId);

    var reads =
        streamOps
            .read(options, StreamOffset.create(streamKey, offset))
            .map(record -> new StreamMessage(record.getId().getValue(), record.getValue()));

    return touchStreamTtl(streamKey, streamTtl)
        .thenMany(withTrimAfterRead(reads, streamKey, streamTtl))
        .onErrorMap(ex -> wrapStreamError("READ_NO_GROUP_FAILED", streamKey, ex));
  }

  @Override
  public Flux<StreamMessage> pollNoGroup(
      String streamKey,
      java.util.function.Supplier<String> lastMessageIdSupplier,
      java.util.function.Consumer<String> lastMessageIdConsumer,
      Duration block,
      int count,
      Duration streamTtl) {
    return pollNoGroup(
        streamKey,
        lastMessageIdSupplier,
        lastMessageIdConsumer,
        block,
        count,
        streamTtl,
        () -> true);
  }

  @Override
  public Flux<StreamMessage> pollNoGroup(
      String streamKey,
      java.util.function.Supplier<String> lastMessageIdSupplier,
      java.util.function.Consumer<String> lastMessageIdConsumer,
      Duration block,
      int count,
      Duration streamTtl,
      BooleanSupplier continuePolling) {

    return Flux.defer(
            () ->
                readNoGroup(streamKey, lastMessageIdSupplier.get(), block, count, streamTtl)
                    .doOnNext(msg -> lastMessageIdConsumer.accept(msg.id())))
        .repeatWhen(
            completed ->
                completed.flatMap(
                    ignored ->
                        continuePolling.getAsBoolean()
                            ? Flux.just(0).delayElements(Duration.ofMillis(50))
                            : Flux.empty()));
  }

  @Override
  public Mono<Void> ensureGroup(String streamKey, String group, Duration streamTtl) {
    TtlPolicy.requirePositive(streamTtl, "ensureGroup");
    return streamOps
        .createGroup(streamKey, ReadOffset.from("0-0"), group)
        .onErrorResume(
            ex -> {
              if (ex instanceof RedisSystemException
                  && Objects.nonNull(ex.getMessage())
                  && ex.getMessage().contains("BUSYGROUP")) {
                return Mono.empty();
              }
              return Mono.error(wrapStreamError("ENSURE_GROUP_FAILED", streamKey, ex));
            })
        .then(touchStreamTtl(streamKey, streamTtl).then())
        .onErrorMap(ex -> wrapStreamError("ENSURE_GROUP_FAILED", streamKey, ex));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Flux<StreamMessage> readGroup(
      String streamKey,
      String group,
      String consumer,
      Duration streamTtl,
      Duration block,
      int count) {

    TtlPolicy.requirePositive(streamTtl, "readGroup");
    var waitTime = Objects.nonNull(block) ? block : Duration.ofSeconds(1);
    var options = StreamReadOptions.empty().block(waitTime).count(Math.max(1, count));

    var reads =
        streamOps
            .read(
                Consumer.from(group, consumer),
                options,
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()))
            .map(r -> new StreamMessage(r.getId().getValue(), r.getValue()));

    return touchStreamTtl(streamKey, streamTtl)
        .thenMany(withTrimAfterRead(reads, streamKey, streamTtl))
        .onErrorMap(ex -> wrapStreamError("READ_GROUP_FAILED", streamKey, ex));
  }

  @Override
  public Mono<Void> ack(String streamKey, String group, String messageId, Duration streamTtl) {
    TtlPolicy.requirePositive(streamTtl, "ack");
    return streamOps
        .acknowledge(streamKey, group, messageId)
        .then(touchStreamTtl(streamKey, streamTtl))
        .onErrorMap(ex -> wrapStreamError("ACK_FAILED", streamKey, ex))
        .then();
  }

  @Override
  public Mono<Void> claimStale(
      String streamKey,
      String group,
      String consumer,
      Duration streamTtl,
      Duration minIdleTime) {

    TtlPolicy.requirePositive(streamTtl, "claimStale");
    TtlPolicy.requirePositive(minIdleTime, "claimStale.minIdleTime");

    return touchStreamTtl(streamKey, streamTtl)
        .thenMany(
            streamOps
                .pending(streamKey, group, org.springframework.data.domain.Range.unbounded(), 100L)
                .flatMapIterable(pendingMessages -> pendingMessages)
                .filter(
                    pendingMsg -> {
                      var idleTime = pendingMsg.getElapsedTimeSinceLastDelivery();
                      return Objects.nonNull(idleTime) && idleTime.compareTo(minIdleTime) >= 0;
                    })
                .map(pendingMsg -> pendingMsg.getId())
                .collectList()
                .flatMapMany(
                    staleMessageIds -> {
                      if (staleMessageIds.isEmpty()) {
                        return Flux.empty();
                      }
                      return streamOps.claim(
                          streamKey,
                          group,
                          consumer,
                          minIdleTime,
                          staleMessageIds.toArray(
                              new org.springframework.data.redis.connection.stream.RecordId[0]));
                    }))
        .then()
        .onErrorMap(ex -> wrapStreamError("CLAIM_STALE_FAILED", streamKey, ex));
  }

  @Override
  public Mono<Void> moveToDlq(
      String originalStreamKey,
      String dlqStreamKey,
      String group,
      String messageId,
      Map<String, String> payload,
      String errorReason,
      Duration dlqRetentionTtl,
      Duration originalStreamTtl) {

    Map<String, String> dlqPayload = new HashMap<>(payload);
    dlqPayload.put("dlq_reason", errorReason);
    dlqPayload.put("original_stream", originalStreamKey);

    return xadd(dlqStreamKey, dlqPayload, dlqRetentionTtl)
        .then(ack(originalStreamKey, group, messageId, originalStreamTtl))
        .doOnSuccess(
            v ->
                log.error(
                    "Moved unprocessable message {} from {} to DLQ {}",
                    messageId,
                    originalStreamKey,
                    dlqStreamKey))
        .onErrorMap(ex -> wrapStreamError("DLQ_ROUTING_FAILED", originalStreamKey, ex));
  }

  private Mono<Boolean> touchStreamTtl(String streamKey, Duration streamTtl) {
    return redisTemplate.expire(streamKey, streamTtl);
  }

  private PRuntimeException wrapStreamError(String condition, String streamKey, Throwable ex) {
    return PRuntimeException.builder()
        .condition(condition)
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .type("REDIS_STREAM_FAILURE")
        .title("Stream Processing Error")
        .detail("Failed stream operation for key: " + streamKey)
        .cause(ex)
        .build();
  }
}
