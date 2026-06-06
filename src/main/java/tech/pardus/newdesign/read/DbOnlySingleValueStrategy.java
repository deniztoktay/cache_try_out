package tech.pardus.newdesign.read;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import reactor.core.publisher.Mono;

/** Always reads single values from the database fallback. */
public final class DbOnlySingleValueStrategy<T> implements SingleValueReadStrategy<T> {

  @Override
  public ReadTier tier() {
    return ReadTier.DB_ONLY;
  }

  @Override
  public Mono<Optional<T>> findById(String id, Mono<T> databaseFallback) {
    if (id == null || id.isBlank()) {
      return Mono.just(Optional.empty());
    }
    return databaseFallback.map(Optional::of).defaultIfEmpty(Optional.empty());
  }

  @Override
  public Mono<T> findByName(String alias, Mono<T> databaseFallback) {
    if (alias == null || alias.isBlank()) {
      return Mono.empty();
    }
    return databaseFallback;
  }

  @Override
  public Mono<List<T>> find(Predicate<T> predicate, Mono<List<T>> databaseFallbackWhenEmpty) {
    if (predicate == null) {
      return Mono.just(List.of());
    }
    return databaseFallbackWhenEmpty
        .map(list -> list == null ? List.<T>of() : list.stream().filter(predicate).toList())
        .defaultIfEmpty(List.of());
  }
}
