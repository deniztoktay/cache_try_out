package tech.pardus.cache.l1;

import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.CacheValueCodec;
import tech.pardus.redis.cache.Identifiable;
import tech.pardus.redis.support.TtlPolicy;

/**
 * Persists an L1 snapshot to Redis (L2) before the L1 cache resizes. Blocking is used inside the
 * resize lock because resize is infrequent and must complete before the larger cache is built.
 */
@Slf4j
public class ReactiveL1RedisPersister<T extends Identifiable<?>> implements L1ResizePersister<String, T> {
  private static final Duration BLOCK_TIMEOUT = Duration.ofMinutes(5);

  private final CacheNamespace namespace;
  private final RedisValueStore valueStore;
  private final CacheIndexStore indexStore;
  private final CacheValueCodec<T> codec;
  private final Duration ttl;

  public ReactiveL1RedisPersister(
      CacheNamespace namespace,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      CacheValueCodec<T> codec,
      Duration ttl) {
    this.namespace = namespace;
    this.valueStore = valueStore;
    this.indexStore = indexStore;
    this.codec = codec;
    this.ttl = TtlPolicy.requirePositive(ttl, "l1Persist");
  }

  @Override
  public void persistBeforeResize(Map<String, T> snapshot) {
    if (snapshot.isEmpty()) {
      return;
    }
    Flux.fromIterable(snapshot.entrySet())
        .concatMap(entry -> persistEntry(entry.getKey(), entry.getValue()))
        .then()
        .block(BLOCK_TIMEOUT);
    log.debug("Persisted {} L1 entries to Redis namespace {}", snapshot.size(), namespace.name());
  }

  private Mono<Void> persistEntry(String stringId, T value) {
    var valueKey = CacheKeyLayout.liveValueKey(namespace, stringId);
    return valueStore
        .setBytes(valueKey, codec.encode(value), ttl)
        .then(indexStore.addMembers(CacheKeyLayout.liveIndexKey(namespace), java.util.List.of(stringId), ttl))
        .then();
  }
}
