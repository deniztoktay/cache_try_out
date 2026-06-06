package tech.pardus.newdesign.redis.store;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.cachekey.CacheKey;
import tech.pardus.newdesign.redis.keys.CacheRedisKeys;

public class ReactiveRedisMetaStore implements RedisMetaStore {

  private static final byte[] READY = "1".getBytes();

  private final ReactiveRedisTemplate<String, byte[]> template;

  public ReactiveRedisMetaStore(ReactiveRedisTemplate<String, byte[]> template) {
    this.template = template;
  }

  @Override
  public Mono<Boolean> isReady(CacheKey cache) {
    return template.hasKey(CacheRedisKeys.metaKey(cache));
  }

  @Override
  public Mono<Void> markReady(CacheKey cache) {
    return template
        .opsForValue()
        .set(CacheRedisKeys.metaKey(cache), READY, cache.ttl())
        .then();
  }

  @Override
  public Mono<Void> clearReady(CacheKey cache) {
    return template.delete(CacheRedisKeys.metaKey(cache)).then();
  }
}
