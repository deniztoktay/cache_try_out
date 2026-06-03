package tech.pardus.cache.l2;

import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.Identifiable;

/** Incremental L2 mutations after JDBC writes. */
public interface L2CacheOperations<M extends Identifiable<?>> {

  Mono<Void> upsert(M model);

  Mono<Void> remove(String primaryMemberId, String aliasMemberIdOrNull);
}
