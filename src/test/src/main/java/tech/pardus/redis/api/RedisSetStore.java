package tech.pardus.redis.api;

import java.time.Duration;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Redis set operations; all writes require a positive TTL on the set key. */
public interface RedisSetStore {

  Mono<Long> addMembers(String setKey, Collection<String> members, Duration ttl);

  Mono<Long> removeMember(String setKey, String member, Duration ttl);

  Flux<String> members(String setKey);
}
