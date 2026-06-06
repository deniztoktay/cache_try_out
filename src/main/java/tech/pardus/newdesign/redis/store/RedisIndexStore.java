package tech.pardus.newdesign.redis.store;

import java.time.Duration;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.cachekey.CacheKey;

/** Member-id index ({@code {root}:idx}) for a cache partition. */
public interface RedisIndexStore {

  Flux<String> members(CacheKey cache);

  Mono<Void> replaceAll(CacheKey cache, Collection<String> memberIds, Duration ttl);

  Mono<Void> addMember(CacheKey cache, String memberId, Duration ttl);

  Mono<Void> removeMember(CacheKey cache, String memberId);
}
