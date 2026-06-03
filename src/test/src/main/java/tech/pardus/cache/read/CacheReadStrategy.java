package tech.pardus.cache.read;

import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.Identifiable;

import tech.pardus.cache.tier.CacheServiceTier;

/**
 * Tier-specific read path for cache models ({@link CacheServiceTier}).
 */
public interface CacheReadStrategy<ID, M extends Identifiable<ID>> {

  Mono<M> getByMemberId(String memberId, Mono<M> databaseFallback);

  Mono<List<M>> getAllIndexed(Mono<List<M>> databaseFallback);

  /** Validation / UQ reads (may bypass cache when leader is initializing L2). */
  Mono<List<M>> loadAllForValidation();

  Mono<Optional<M>> findByIdForValidation(ID id);

  /** L1_L2 stream consumer: L2 then DB, never L1. */
  Mono<M> loadForL1Projection(ID id);

  boolean supportsL1();
}
