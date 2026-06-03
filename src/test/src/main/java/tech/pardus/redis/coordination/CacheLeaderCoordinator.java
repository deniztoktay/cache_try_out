package tech.pardus.redis.coordination;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.config.CacheCoordinationProperties;
import tech.pardus.redis.service.ReactiveWithLockExecutor;

/**
 * Runs cache maintenance on a single pod using renewable Redis locks. Long init/groom operations
 * automatically extend the lock TTL via {@link ReactiveWithLockExecutor#withRenewableLockIfAvailable}.
 */
@Slf4j
@Component
public class CacheLeaderCoordinator {
  private final ReactiveWithLockExecutor lockExecutor;
  private final CacheCoordinationProperties properties;
  private final CacheL2ReadyMarker readyMarker;

  public CacheLeaderCoordinator(
      ReactiveWithLockExecutor lockExecutor,
      CacheCoordinationProperties properties,
      CacheL2ReadyMarker readyMarker) {
    this.lockExecutor = lockExecutor;
    this.properties = properties;
    this.readyMarker = readyMarker;
  }

  /**
   * Leader pod: runs L2 work under a renewable init lock, then warms L1. Follower pods wait for the
   * L2 ready marker, then warm L1.
   */
  public Mono<Void> runStartup(
      CacheNamespace namespace,
      Duration readyMarkerTtl,
      Mono<Void> leaderL2Work,
      Mono<Void> l1WarmWork) {
    var lockKey = CacheKeyLayout.initLockKey(namespace);
    var lockTtl = properties.getInitLockTtl();

    return lockExecutor
        .withRenewableLockIfAvailable(
            lockKey,
            lockTtl,
            () -> runAsLeader(namespace, readyMarkerTtl, leaderL2Work).then(l1WarmWork))
        .switchIfEmpty(runAsFollower(namespace, l1WarmWork));
  }

  /** Runs grooming only on the pod that acquires the renewable groom lock. */
  public Mono<Void> runGroomingIfLeader(CacheNamespace namespace, Mono<Void> task) {
    var lockKey = CacheKeyLayout.groomLockKey(namespace);
    return lockExecutor
        .withRenewableLockIfAvailable(lockKey, properties.getGroomLockTtl(), () -> task)
        .doOnSubscribe(s -> log.debug("Grooming leader for namespace {}", namespace.name()))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.trace("Skipping grooming for {}, lock held by another pod", namespace.name());
                  return Mono.empty();
                }))
        .then();
  }

  public Mono<Void> awaitL2Ready(CacheNamespace namespace) {
    return Flux.interval(properties.getL2ReadyPollInterval())
        .concatMap(tick -> readyMarker.isReady(namespace))
        .filter(Boolean::booleanValue)
        .next()
        .timeout(properties.getL2ReadyWaitTimeout())
        .then()
        .doOnSubscribe(s -> log.info("Waiting for L2 ready marker on namespace {}", namespace.name()))
        .doOnSuccess(v -> log.info("L2 ready for namespace {}", namespace.name()));
  }

  private Mono<Void> runAsLeader(
      CacheNamespace namespace, Duration readyMarkerTtl, Mono<Void> leaderWork) {
    log.info(
        "Pod {} elected cache leader for {} (renewable lock)",
        lockExecutor.ownerId(),
        namespace.name());
    return readyMarker
        .clearReady(namespace)
        .then(leaderWork)
        .then(readyMarker.markReady(namespace, readyMarkerTtl))
        .doOnSuccess(v -> log.info("Leader finished L2 population for {}", namespace.name()));
  }

  private Mono<Void> runAsFollower(CacheNamespace namespace, Mono<Void> followerWork) {
    log.info(
        "Pod {} is cache follower for {}, awaiting L2",
        lockExecutor.ownerId(),
        namespace.name());
    return awaitL2Ready(namespace).then(followerWork);
  }
}
