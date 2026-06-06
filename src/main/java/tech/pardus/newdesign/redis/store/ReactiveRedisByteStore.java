package tech.pardus.newdesign.redis.store;

import java.time.Duration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

/** {@link RedisByteStore} backed by Spring Data Redis (connection only). */
public class ReactiveRedisByteStore implements RedisByteStore {

  private final ReactiveRedisTemplate<String, byte[]> template;

  public ReactiveRedisByteStore(ReactiveRedisTemplate<String, byte[]> template) {
    this.template = template;
  }

  @Override
  public Mono<byte[]> get(String key) {
    return template
        .opsForValue()
        .get(key)
        .flatMap(
            bytes ->
                bytes == null || bytes.length == 0 ? Mono.empty() : Mono.just(bytes));
  }

  @Override
  public Mono<Void> set(String key, byte[] payload, Duration ttl) {
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      return template.opsForValue().set(key, payload).then();
    }
    return template.opsForValue().set(key, payload, ttl).then();
  }

  @Override
  public Mono<Void> delete(String key) {
    return template.delete(key).then();
  }
}
