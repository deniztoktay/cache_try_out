package tech.pardus.redis.api;

import java.time.Duration;
import java.util.List;
import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.CacheValueCodec;
import tech.pardus.redis.cache.Identifiable;

/**
 * Blue-green cache load: writes a temporary index + values, then atomically promotes them to the
 * live namespace so readers always see either the previous or the new dataset.
 */
public interface CacheInitializer {

  <T extends Identifiable<?>> Mono<Void> initialize(
      CacheNamespace namespace, Duration ttl, Mono<List<T>> dbFetchTask, CacheValueCodec<T> codec);

  <T extends Identifiable<?>> Mono<Void> initialize(
      CacheNamespace namespace, Duration ttl, List<T> prefetched, CacheValueCodec<T> codec);
}
