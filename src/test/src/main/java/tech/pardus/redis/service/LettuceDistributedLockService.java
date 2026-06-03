package tech.pardus.redis.service;

import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.DistributedLockService;
import tech.pardus.redis.dto.LockHandle;

public class LettuceDistributedLockService implements DistributedLockService {
  private static final RedisScript<Long> UNLOCK_SCRIPT =
      RedisScript.of(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "return redis.call('del', KEYS[1]) "
              + "else return 0 end",
          Long.class);

  private static final RedisScript<Long> REFRESH_SCRIPT =
      RedisScript.of(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "return redis.call('pexpire', KEYS[1], ARGV[2]) "
              + "else return 0 end",
          Long.class);

  private final ReactiveStringRedisTemplate redis;

  public LettuceDistributedLockService(ReactiveStringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Mono<LockHandle> tryLock(String lockKey, String ownerId, Duration ttl) {
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      return Mono.empty();
    }
    return redis
        .opsForValue()
        .setIfAbsent(lockKey, ownerId, ttl)
        .flatMap(acquired -> acquired ? Mono.just(new LockHandle(lockKey, ownerId)) : Mono.empty());
  }

  @Override
  public Mono<Boolean> refresh(LockHandle handle, Duration ttl) {
    if (handle == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
      return Mono.just(false);
    }
    long ttlMs = ttl.toMillis();
    return redis
        .execute(REFRESH_SCRIPT, List.of(handle.lockKey()), handle.ownerId(), Long.toString(ttlMs))
        .singleOrEmpty()
        .map(result -> result != null && result > 0)
        .defaultIfEmpty(false);
  }

  @Override
  public Mono<Boolean> unlock(LockHandle handle) {
    if (handle == null) {
      return Mono.just(false);
    }
    return redis
        .execute(UNLOCK_SCRIPT, List.of(handle.lockKey()), handle.ownerId())
        .singleOrEmpty()
        .map(result -> result != null && result > 0)
        .defaultIfEmpty(false);
  }
}
