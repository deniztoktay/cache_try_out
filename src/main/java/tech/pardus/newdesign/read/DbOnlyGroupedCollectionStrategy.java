package tech.pardus.newdesign.read;

import java.util.List;
import reactor.core.publisher.Mono;

/** Always reads grouped collections from the database fallback. */
public final class DbOnlyGroupedCollectionStrategy<T> implements GroupedCollectionReadStrategy<T> {

  @Override
  public ReadTier tier() {
    return ReadTier.DB_ONLY;
  }

  @Override
  public Mono<List<T>> findByValueGroup(String groupId, Mono<List<T>> databaseFallback) {
    if (groupId == null || groupId.isBlank()) {
      return Mono.just(List.of());
    }
    return databaseFallback.defaultIfEmpty(List.of());
  }

  @Override
  public Mono<List<T>> findByNamedGroup(String groupAlias, Mono<List<T>> databaseFallback) {
    if (groupAlias == null || groupAlias.isBlank()) {
      return Mono.just(List.of());
    }
    return databaseFallback.defaultIfEmpty(List.of());
  }
}
