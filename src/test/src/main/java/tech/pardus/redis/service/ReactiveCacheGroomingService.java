package tech.pardus.redis.service;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.CacheGroomingService;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.support.TtlPolicy;

/** Index-driven grooming: drops index members whose value key no longer exists in Redis. */
@Slf4j
@Component
public class ReactiveCacheGroomingService implements CacheGroomingService {
  private final CacheIndexStore index;
  private final RedisValueStore values;

  public ReactiveCacheGroomingService(CacheIndexStore index, RedisValueStore values) {
    this.index = index;
    this.values = values;
  }

  @Override
  public Mono<Long> groomByIndex(CacheNamespace namespace, Duration indexTtl) {
    TtlPolicy.requirePositive(indexTtl, "groomByIndex");
    var indexKey = CacheKeyLayout.liveIndexKey(namespace);

    return index
        .listMembers(indexKey)
        .flatMap(
            memberId -> {
              var valueKey = CacheKeyLayout.liveValueKey(namespace, memberId);
              return values
                  .getBytes(valueKey)
                  .map(present -> 0L)
                  .switchIfEmpty(
                      index
                          .removeMember(indexKey, memberId, indexTtl)
                          .doOnSuccess(
                              removed ->
                                  log.info(
                                      "Groomed index member {} (missing value) in namespace {}",
                                      memberId,
                                      namespace.name()))
                          .thenReturn(1L));
            })
        .reduce(0L, Long::sum);
  }
}
