package tech.pardus.newdesign.redis.read;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.cachekey.CacheKey;
import tech.pardus.newdesign.redis.codec.ValuePayloadCodec;
import tech.pardus.newdesign.redis.keys.CacheRedisKeys;
import tech.pardus.newdesign.redis.store.RedisByteStore;

/** Read-through access to a single entity at {@code v:{id}} or {@code v:n:{alias}}. */
@Slf4j
public class RedisSingleValueReader<T> {

  private final CacheKey cacheKey;
  private final RedisByteStore store;
  private final ValuePayloadCodec<T> codec;
  private final boolean readThrough;

  public RedisSingleValueReader(CacheKey cacheKey, RedisByteStore store, ValuePayloadCodec<T> codec) {
    this(cacheKey, store, codec, true);
  }

  public RedisSingleValueReader(
      CacheKey cacheKey, RedisByteStore store, ValuePayloadCodec<T> codec, boolean readThrough) {
    this.cacheKey = cacheKey;
    this.store = store;
    this.codec = codec;
    this.readThrough = readThrough;
  }

  public Mono<T> readById(String memberId, Mono<T> databaseFallback) {
    return read(CacheRedisKeys.valueKey(cacheKey, memberId), memberId, databaseFallback);
  }

  public Mono<T> readByName(String alias, Mono<T> databaseFallback) {
    return read(CacheRedisKeys.namedKey(cacheKey, alias), alias, databaseFallback);
  }

  private Mono<T> read(String redisKey, String logicalKey, Mono<T> databaseFallback) {
    return store
        .get(redisKey)
        .map(bytes -> codec.decode(bytes, redisKey))
        .switchIfEmpty(loadAndMaybeCache(redisKey, logicalKey, databaseFallback))
        .onErrorResume(
            ex -> {
              log.warn(
                  "Redis value read failed for cache={} key={}", cacheKey.getKey(), logicalKey, ex);
              return loadAndMaybeCache(redisKey, logicalKey, databaseFallback);
            });
  }

  private Mono<T> loadAndMaybeCache(String redisKey, String logicalKey, Mono<T> databaseFallback) {
    return databaseFallback.flatMap(
        value -> {
          if (!readThrough || value == null) {
            return Mono.justOrEmpty(value);
          }
          return store
              .set(redisKey, codec.encode(value), cacheKey.ttl())
              .thenReturn(value)
              .doOnSubscribe(
                  s ->
                      log.debug(
                          "Value cache miss for cache={} key={}", cacheKey.getKey(), logicalKey));
        });
  }
}
