package tech.pardus.newdesign.redis.store;

import reactor.core.publisher.Mono;
import tech.pardus.newdesign.cachekey.CacheKey;

/** Bulk-load ready marker ({@code {root}:meta}). */
public interface RedisMetaStore {

  Mono<Boolean> isReady(CacheKey cache);

  Mono<Void> markReady(CacheKey cache);

  Mono<Void> clearReady(CacheKey cache);
}
