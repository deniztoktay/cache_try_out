package tech.pardus.redis.cache;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;

/**
 * Redis-first reads with database fallback when a value is missing or Redis errors.
 */
@Slf4j
public class ReactiveCacheReader<T extends Identifiable<?>> {
  private final CacheNamespace namespace;
  private final RedisValueStore values;
  private final CacheIndexStore index;
  private final CacheValueCodec<T> codec;

  public ReactiveCacheReader(
      CacheNamespace namespace,
      RedisValueStore values,
      CacheIndexStore index,
      CacheValueCodec<T> codec) {
    this.namespace = namespace;
    this.values = values;
    this.index = index;
    this.codec = codec;
  }

  public Mono<T> getByMemberId(String memberId, Mono<T> databaseFallback) {
    return readValue(memberId)
        .switchIfEmpty(
            databaseFallback.doOnSubscribe(s -> log.debug("Cache miss for member {}, loading DB", memberId)))
        .onErrorResume(
            ex -> {
              log.warn("Redis read failed for member {}, falling back to DB", memberId, ex);
              return databaseFallback;
            });
  }

  public Mono<List<T>> getAllIndexed(Mono<List<T>> databaseFallback) {
    var indexKey = CacheKeyLayout.liveIndexKey(namespace);
    return index
        .listMembers(indexKey)
        .flatMap(this::readValue)
        .collectList()
        .flatMap(
            cached -> {
              if (cached.isEmpty()) {
                return databaseFallback;
              }
              return index
                  .listMembers(indexKey)
                  .count()
                  .flatMap(
                      indexSize -> {
                        if (indexSize != cached.size()) {
                          log.warn(
                              "Cache index size {} != resolved values {}, falling back to DB",
                              indexSize,
                              cached.size());
                          return databaseFallback;
                        }
                        return Mono.just(cached);
                      });
            })
        .onErrorResume(
            ex -> {
              log.warn("Redis read-all failed for namespace {}, falling back to DB", namespace.name(), ex);
              return databaseFallback;
            });
  }

  private Mono<T> readValue(String memberId) {
    return values
        .getBytes(CacheKeyLayout.liveValueKey(namespace, memberId))
        .map(bytes -> codec.decode(bytes, memberId));
  }
}
