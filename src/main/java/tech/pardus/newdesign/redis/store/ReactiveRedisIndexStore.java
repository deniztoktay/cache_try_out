package tech.pardus.newdesign.redis.store;

import java.time.Duration;
import java.util.Collection;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.cachekey.CacheKey;
import tech.pardus.newdesign.redis.keys.CacheRedisKeys;

public class ReactiveRedisIndexStore implements RedisIndexStore {

  private final ReactiveStringRedisTemplate template;

  public ReactiveRedisIndexStore(ReactiveStringRedisTemplate template) {
    this.template = template;
  }

  @Override
  public Flux<String> members(CacheKey cache) {
    return template.opsForSet().members(CacheRedisKeys.indexKey(cache));
  }

  @Override
  public Mono<Void> replaceAll(CacheKey cache, Collection<String> memberIds, Duration ttl) {
    var key = CacheRedisKeys.indexKey(cache);
    return template
        .delete(key)
        .then(
            memberIds == null || memberIds.isEmpty()
                ? Mono.empty()
                : template.opsForSet().add(key, memberIds.toArray(String[]::new)).then())
        .then(ttl == null || ttl.isZero() ? Mono.empty() : template.expire(key, ttl))
        .then();
  }

  @Override
  public Mono<Void> addMember(CacheKey cache, String memberId, Duration ttl) {
    if (memberId == null || memberId.isBlank()) {
      return Mono.empty();
    }
    var key = CacheRedisKeys.indexKey(cache);
    return template
        .opsForSet()
        .add(key, memberId)
        .then(ttl == null || ttl.isZero() ? Mono.empty() : template.expire(key, ttl))
        .then();
  }

  @Override
  public Mono<Void> removeMember(CacheKey cache, String memberId) {
    if (memberId == null || memberId.isBlank()) {
      return Mono.empty();
    }
    return template.opsForSet().remove(CacheRedisKeys.indexKey(cache), memberId).then();
  }
}
