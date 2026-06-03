package tech.pardus.cache.read;

import java.util.List;
import java.util.Optional;

/** JDBC validation reads with tier-aware cache fallback. */
public interface ValidationEntityView<ID, V> {

  List<V> findAllForValidation();

  Optional<V> findByIdForValidation(ID id);
}
