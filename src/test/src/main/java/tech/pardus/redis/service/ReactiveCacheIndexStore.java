package tech.pardus.redis.service;

import java.time.Duration;
import java.util.Collection;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.support.RedisErrors;
import tech.pardus.redis.api.RedisSetStore;
import tech.pardus.redis.support.TtlPolicy;

@Component
public class ReactiveCacheIndexStore implements CacheIndexStore {
  private final RedisSetStore sets;
  private final ReactiveStringRedisTemplate redis;

  public ReactiveCacheIndexStore(RedisSetStore sets, ReactiveStringRedisTemplate redis) {
    this.sets = sets;
    this.redis = redis;
  }

  @Override
  public Mono<Long> addMembers(String indexKey, Collection<String> memberIds, Duration ttl) {
    return sets.addMembers(indexKey, memberIds, ttl);
  }

  @Override
  public Mono<Long> removeMember(String indexKey, String memberId, Duration ttl) {
    return sets.removeMember(indexKey, memberId, ttl);
  }

  @Override
  public Flux<String> listMembers(String indexKey) {
    return sets.members(indexKey);
  }

  @Override
  public Mono<Boolean> touchTtl(String indexKey, Duration ttl) {
    TtlPolicy.requirePositive(ttl, "touchTtl");
    return redis
        .expire(indexKey, ttl)
        .onErrorResume(ex -> RedisErrors.isNoSuchKey(ex) ? Mono.just(false) : Mono.error(ex));
  }
}
