package tech.pardus.newdesign.redis.read;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.cachekey.CacheKey;
import tech.pardus.newdesign.redis.codec.ListPayloadCodec;
import tech.pardus.newdesign.redis.keys.CacheRedisKeys;
import tech.pardus.newdesign.redis.store.RedisByteStore;

/**
 * Read-through grouped collections stored at {@code v:{groupId}} or {@code v:n:{groupAlias}}.
 *
 * <p>AttributeSetting example:
 *
 * <ul>
 *   <li>{@code findByAttributeId(1)} → {@code puurs:cache:attributesetting:v:1}
 *   <li>{@code findByReferenceTypeId(2)} → {@code puurs:cache:attributesetting:v:n:2}
 * </ul>
 */
@Slf4j
public class RedisGroupedCollectionReader<T> {

  private final CacheKey cacheKey;
  private final RedisByteStore store;
  private final ListPayloadCodec<T> codec;
  private final boolean readThrough;

  public RedisGroupedCollectionReader(
      CacheKey cacheKey, RedisByteStore store, ListPayloadCodec<T> codec) {
    this(cacheKey, store, codec, true);
  }

  public RedisGroupedCollectionReader(
      CacheKey cacheKey, RedisByteStore store, ListPayloadCodec<T> codec, boolean readThrough) {
    this.cacheKey = cacheKey;
    this.store = store;
    this.codec = codec;
    this.readThrough = readThrough;
  }

  /** Reads collection grouped by primary id ({@code v:{groupId}}). */
  public Mono<List<T>> readByValueGroup(String groupId, Mono<List<T>> databaseFallback) {
    return read(CacheRedisKeys.valueKey(cacheKey, groupId), groupId, databaseFallback);
  }

  /** Reads collection grouped by named/alternate key ({@code v:n:{groupAlias}}). */
  public Mono<List<T>> readByNamedGroup(String groupAlias, Mono<List<T>> databaseFallback) {
    return read(CacheRedisKeys.namedKey(cacheKey, groupAlias), groupAlias, databaseFallback);
  }

  private Mono<List<T>> read(String redisKey, String logicalKey, Mono<List<T>> databaseFallback) {
    return store
        .get(redisKey)
        .map(bytes -> codec.decode(bytes, redisKey))
        .switchIfEmpty(loadAndMaybeCache(redisKey, logicalKey, databaseFallback))
        .onErrorResume(
            ex -> {
              log.warn(
                  "Redis collection read failed for cache={} key={}",
                  cacheKey.getKey(),
                  logicalKey,
                  ex);
              return loadAndMaybeCache(redisKey, logicalKey, databaseFallback);
            });
  }

  private Mono<List<T>> loadAndMaybeCache(
      String redisKey, String logicalKey, Mono<List<T>> databaseFallback) {
    return databaseFallback.flatMap(
        list -> {
          if (!readThrough || list == null || list.isEmpty()) {
            return Mono.justOrEmpty(list);
          }
          return store
              .set(redisKey, codec.encode(list), cacheKey.ttl())
              .thenReturn(list)
              .doOnSubscribe(
                  s ->
                      log.debug(
                          "Collection cache miss for cache={} key={}",
                          cacheKey.getKey(),
                          logicalKey));
        });
  }
}
