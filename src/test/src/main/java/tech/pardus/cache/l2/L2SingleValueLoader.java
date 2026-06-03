package tech.pardus.cache.l2;

import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.CacheValueCodec;
import tech.pardus.redis.cache.Identifiable;

/** Loads indexed models from Redis L2 without L1. */
public class L2SingleValueLoader<M extends Identifiable<?>> {

  private final CacheNamespace namespace;
  private final RedisValueStore valueStore;
  private final CacheIndexStore indexStore;
  private final CacheValueCodec<M> codec;

  public L2SingleValueLoader(
      CacheNamespace namespace,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      CacheValueCodec<M> codec) {
    this.namespace = namespace;
    this.valueStore = valueStore;
    this.indexStore = indexStore;
    this.codec = codec;
  }

  public Mono<Optional<M>> loadByMemberId(String memberId) {
    return valueStore
        .getBytes(CacheKeyLayout.liveValueKey(namespace, memberId))
        .map(bytes -> Optional.of(codec.decode(bytes, memberId)))
        .switchIfEmpty(Mono.empty());
  }

  public Mono<List<M>> loadAllIndexed() {
    var indexKey = CacheKeyLayout.liveIndexKey(namespace);
    return indexStore
        .listMembers(indexKey)
        .flatMap(
            stringId ->
                valueStore
                    .getBytes(CacheKeyLayout.liveValueKey(namespace, stringId))
                    .map(bytes -> codec.decode(bytes, stringId)))
        .collectList()
        .flatMap(models -> models.isEmpty() ? Mono.empty() : Mono.just(models));
  }
}
