package tech.pardus.redis.coordination;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.RedisKeyMaintenance;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.support.TtlPolicy;

@Component
public class CacheL2ReadyMarker {
  private static final byte[] READY_PAYLOAD = "READY".getBytes(StandardCharsets.UTF_8);

  private final RedisValueStore valueStore;
  private final RedisKeyMaintenance maintenance;

  public CacheL2ReadyMarker(RedisValueStore valueStore, RedisKeyMaintenance maintenance) {
    this.valueStore = valueStore;
    this.maintenance = maintenance;
  }

  public Mono<Void> clearReady(CacheNamespace namespace) {
    return maintenance.deleteKey(CacheKeyLayout.l2ReadyKey(namespace)).then();
  }

  public Mono<Void> markReady(CacheNamespace namespace, Duration ttl) {
    TtlPolicy.requirePositive(ttl, "markL2Ready");
    return valueStore
        .setBytes(CacheKeyLayout.l2ReadyKey(namespace), READY_PAYLOAD, ttl)
        .then();
  }

  public Mono<Boolean> isReady(CacheNamespace namespace) {
    return valueStore
        .getBytes(CacheKeyLayout.l2ReadyKey(namespace))
        .map(bytes -> bytes != null && bytes.length > 0)
        .defaultIfEmpty(false);
  }
}
