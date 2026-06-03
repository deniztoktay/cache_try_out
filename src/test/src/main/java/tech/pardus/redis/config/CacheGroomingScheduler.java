package tech.pardus.redis.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.pardus.redis.api.CacheGroomingService;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.coordination.CacheLeaderCoordinator;
import tech.pardus.redis.runtime.RedisRuntimeGate;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "pardus.cache.grooming", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheGroomingScheduler {
  private final CacheGroomingService grooming;
  private final CacheGroomingProperties properties;
  private final CacheLeaderCoordinator leaderCoordinator;
  private final RedisRuntimeGate redisGate;

  public CacheGroomingScheduler(
      CacheGroomingService grooming,
      CacheGroomingProperties properties,
      CacheLeaderCoordinator leaderCoordinator,
      RedisRuntimeGate redisGate) {
    this.grooming = grooming;
    this.properties = properties;
    this.leaderCoordinator = leaderCoordinator;
    this.redisGate = redisGate;
  }

  @Scheduled(fixedDelayString = "${pardus.cache.grooming.interval-ms:300000}")
  public void groomRegisteredNamespaces() {
    if (!redisGate.isOpen()) {
      return;
    }
    List<String> namespaces = properties.getNamespaces();
    if (namespaces.isEmpty()) {
      return;
    }
    for (String name : namespaces) {
      var namespace = new CacheNamespace(name);
      leaderCoordinator
          .runGroomingIfLeader(
              namespace,
              grooming
                  .groomByIndex(namespace, properties.getIndexTtl())
                  .doOnSuccess(
                      count ->
                          log.debug(
                              "Grooming finished for {} ({} index entries removed)", name, count))
                  .then())
          .doOnError(ex -> log.warn("Grooming failed for namespace {}", name, ex))
          .subscribe();
    }
  }
}
