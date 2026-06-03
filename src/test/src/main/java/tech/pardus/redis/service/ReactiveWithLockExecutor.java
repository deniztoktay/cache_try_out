package tech.pardus.redis.service;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.redis.api.DistributedLockService;
import tech.pardus.redis.dto.LockHandle;

@Slf4j
@Component
public class ReactiveWithLockExecutor {
  private final DistributedLockService locks;
  private final String ownerId;

  public ReactiveWithLockExecutor(
      DistributedLockService locks, @Qualifier("redisOwnerId") String ownerId) {
    this.locks = locks;
    this.ownerId = ownerId;
  }

  public String ownerId() {
    return ownerId;
  }

  public <T> Mono<T> withLock(String lockKey, Duration ttl, Mono<T> task) {
    return withRenewableLock(lockKey, ttl, () -> task);
  }

  /**
   * Acquires a distributed lock and automatically renews it in the background until the provided
   * task completes, errors, or is cancelled.
   */
  public <T> Mono<T> withRenewableLock(
      String lockKey, Duration ttl, Supplier<Mono<T>> taskSupplier) {
    return locks
        .tryLock(lockKey, ownerId, ttl)
        .switchIfEmpty(lockNotAcquired(lockKey))
        .flatMap(handle -> runWithRenewal(lockKey, ttl, handle, taskSupplier));
  }

  /**
   * Same as {@link #withRenewableLock} but returns empty when the lock is already held (no error).
   */
  public <T> Mono<T> withRenewableLockIfAvailable(
      String lockKey, Duration ttl, Supplier<Mono<T>> taskSupplier) {
    return locks
        .tryLock(lockKey, ownerId, ttl)
        .flatMap(handle -> runWithRenewal(lockKey, ttl, handle, taskSupplier));
  }

  private <T> Mono<T> runWithRenewal(
      String lockKey, Duration ttl, LockHandle handle, Supplier<Mono<T>> taskSupplier) {
    return Mono.usingWhen(
        Mono.just(handle),
        h -> {
          var refreshInterval = ttl.dividedBy(3);
          Disposable renewalTask =
              Flux.interval(refreshInterval)
                  .flatMap(
                      tick ->
                          locks
                              .refresh(h, ttl)
                              .doOnNext(
                                  renewed -> {
                                    if (Boolean.TRUE.equals(renewed)) {
                                      log.debug(
                                          "Renewed lock {} for owner {}", lockKey, ownerId);
                                    } else {
                                      log.warn(
                                          "Lock renewal failed for {} (stolen or expired)",
                                          lockKey);
                                    }
                                  })
                              .onErrorResume(
                                  err -> {
                                    log.error(
                                        "Lock renewal error for {} owner {}", lockKey, ownerId, err);
                                    return Mono.just(false);
                                  }))
                  .subscribe();

          return Mono.defer(taskSupplier)
              .doFinally(
                  signal -> {
                    log.debug(
                        "Stopping lock renewal for {} due to {}", lockKey, signal);
                    if (Objects.nonNull(renewalTask) && !renewalTask.isDisposed()) {
                      renewalTask.dispose();
                    }
                  });
        },
        h -> unlock(lockKey, h),
        (h, err) -> unlock(lockKey, h),
        h -> unlock(lockKey, h));
  }

  private Mono<Void> unlock(String lockKey, LockHandle handle) {
    log.debug("Releasing lock {} for owner {}", lockKey, ownerId);
    return locks.unlock(handle).then();
  }

  private <T> Mono<T> lockNotAcquired(String lockKey) {
    return Mono.error(
        () ->
            PRuntimeException.builder()
                .condition("LOCK_ACQUISITION_FAILED")
                .status(HttpStatus.CONFLICT)
                .type("DISTRIBUTED_LOCK_UNAVAILABLE")
                .title("Resource Locked")
                .detail("Could not acquire lock: " + lockKey + " for owner: " + ownerId)
                .build());
  }
}
