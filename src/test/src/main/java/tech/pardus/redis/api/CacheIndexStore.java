package tech.pardus.redis.api;

import java.time.Duration;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Set-based index of cache member ids; every mutating operation requires TTL on the index key. */
public interface CacheIndexStore {

  Mono<Long> addMembers(String indexKey, Collection<String> memberIds, Duration ttl);

  Mono<Long> removeMember(String indexKey, String memberId, Duration ttl);

  Flux<String> listMembers(String indexKey);

  Mono<Boolean> touchTtl(String indexKey, Duration ttl);
}
