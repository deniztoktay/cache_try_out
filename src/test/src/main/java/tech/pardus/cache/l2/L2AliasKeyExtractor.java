package tech.pardus.cache.l2;

import java.util.Optional;
import tech.pardus.redis.cache.Identifiable;

/** Optional secondary L2/L1 member id (e.g. name alias). */
@FunctionalInterface
public interface L2AliasKeyExtractor<M extends Identifiable<?>> {

  Optional<String> aliasMemberId(M model);
}
