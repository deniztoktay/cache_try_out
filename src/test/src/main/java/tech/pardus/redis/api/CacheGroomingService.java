package tech.pardus.redis.api;

import java.time.Duration;
import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.CacheNamespace;

/**
 * Removes index members whose value keys are missing (deleted values discovered via the index).
 */
public interface CacheGroomingService {

  Mono<Long> groomByIndex(CacheNamespace namespace, Duration indexTtl);
}
