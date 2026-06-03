package tech.pardus.redis.service;

import java.time.Duration;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.redis.api.RedisSetStore;
import tech.pardus.redis.support.TtlPolicy;

@Slf4j
public class ReactiveRedisSetStore implements RedisSetStore {
  private final ReactiveStringRedisTemplate redis;

  public ReactiveRedisSetStore(ReactiveStringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Mono<Long> addMembers(String setKey, Collection<String> members, Duration ttl) {
    TtlPolicy.requirePositive(ttl, "addMembers");
    if (members.isEmpty()) {
      return Mono.just(0L);
    }
    return redis
        .opsForSet()
        .add(setKey, members.toArray(String[]::new))
        .flatMap(added -> redis.expire(setKey, ttl).thenReturn(added))
        .onErrorMap(ex -> wrapSetError("SET_ADD_ERROR", setKey, ex));
  }

  @Override
  public Mono<Long> removeMember(String setKey, String member, Duration ttl) {
    TtlPolicy.requirePositive(ttl, "removeMember");
    return redis
        .opsForSet()
        .remove(setKey, member)
        .flatMap(removed -> redis.expire(setKey, ttl).thenReturn(removed))
        .onErrorMap(ex -> wrapSetError("SET_REMOVE_ERROR", setKey, ex));
  }

  @Override
  public Flux<String> members(String setKey) {
    return redis.opsForSet().members(setKey).onErrorMap(ex -> wrapSetError("SET_MEMBERS_ERROR", setKey, ex));
  }

  private PRuntimeException wrapSetError(String condition, String key, Throwable ex) {
    log.error("Redis set operation failed [{}]: key={}", condition, key, ex);
    return PRuntimeException.builder()
        .condition(condition)
        .status(HttpStatus.SERVICE_UNAVAILABLE)
        .type("REDIS_SET_FAILURE")
        .title("Set cache error")
        .detail("Failed Redis set operation on key: " + key)
        .cause(ex)
        .build();
  }
}
