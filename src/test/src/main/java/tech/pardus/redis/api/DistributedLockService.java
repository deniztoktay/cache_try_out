package tech.pardus.redis.api;

import java.time.Duration;
import reactor.core.publisher.Mono;
import tech.pardus.redis.dto.LockHandle;

public interface DistributedLockService {
  Mono<LockHandle> tryLock(String lockKey, String ownerId, Duration ttl);

  Mono<Boolean> refresh(LockHandle handle, Duration ttl);

  Mono<Boolean> unlock(LockHandle handle);
}
