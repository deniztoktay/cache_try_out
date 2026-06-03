package tech.pardus.redis.service;

import java.util.Collection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.RedisKeyMaintenance;
import tech.pardus.redis.support.RedisErrors;

@Component
public class ReactiveRedisKeyMaintenance implements RedisKeyMaintenance {
  private final ReactiveRedisTemplate<String, byte[]> redis;

  public ReactiveRedisKeyMaintenance(ReactiveRedisTemplate<String, byte[]> redis) {
    this.redis = redis;
  }

  @Override
  public Mono<Boolean> deleteKey(String key) {
    return redis.delete(key).map(deleted -> deleted != null && deleted > 0);
  }

  @Override
  public Mono<Long> deleteKeys(Collection<String> keys) {
    if (keys.isEmpty()) {
      return Mono.just(0L);
    }
    return Flux.fromIterable(keys).flatMap(key -> redis.delete(key)).reduce(0L, Long::sum);
  }

  @Override
  public Mono<Boolean> renameKey(String fromKey, String toKey) {
    return redis
        .rename(fromKey, toKey)
        .thenReturn(Boolean.TRUE)
        .onErrorResume(
            ex -> RedisErrors.isNoSuchKey(ex) ? Mono.just(false) : Mono.error(ex));
  }
}
