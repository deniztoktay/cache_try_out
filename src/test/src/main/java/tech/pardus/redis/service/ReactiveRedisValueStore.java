package tech.pardus.redis.service;

import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.support.TtlPolicy;

@Slf4j
public class ReactiveRedisValueStore implements RedisValueStore {
  private final ReactiveRedisTemplate<String, byte[]> redis;

  public ReactiveRedisValueStore(ReactiveRedisTemplate<String, byte[]> redis) {
    this.redis = redis;
  }

  @Override
  public Mono<Boolean> setBytes(String key, byte[] value, Duration ttl) {
    TtlPolicy.requirePositive(ttl, "setBytes");
    return redis
        .opsForValue()
        .set(key, value, ttl)
        .onErrorMap(ex -> wrapRedisError("SET_BYTES_ERROR", key, ex));
  }

  @Override
  public Mono<byte[]> getBytes(String key) {
    return redis
        .opsForValue()
        .get(key)
        .onErrorMap(ex -> wrapRedisError("GET_BYTES_ERROR", key, ex));
  }

  @Override
  public Mono<Boolean> setHash(String key, Map<String, byte[]> fields, Duration ttl) {
    TtlPolicy.requirePositive(ttl, "setHash");
    return redis
        .opsForHash()
        .putAll(key, fields)
        .then(redis.expire(key, ttl))
        .onErrorMap(ex -> wrapRedisError("SET_HASH_ERROR", key, ex));
  }

  @Override
  public Mono<Map<String, byte[]>> getHash(String key) {
    return redis
        .opsForHash()
        .entries(key)
        .collectMap(e -> (String) e.getKey(), e -> (byte[]) e.getValue())
        .onErrorMap(ex -> wrapRedisError("GET_HASH_ERROR", key, ex));
  }

  private PRuntimeException wrapRedisError(String condition, String key, Throwable ex) {
    log.error("Redis Operation Failed [{}]: Key={}", condition, key, ex);
    return PRuntimeException.builder()
        .condition(condition)
        .status(HttpStatus.SERVICE_UNAVAILABLE)
        .type("REDIS_DATA_ACCESS_FAILURE")
        .title("Cache Accessibility Error")
        .detail("Failed to perform Redis operation on key: " + key)
        .cause(ex)
        .build();
  }
}
