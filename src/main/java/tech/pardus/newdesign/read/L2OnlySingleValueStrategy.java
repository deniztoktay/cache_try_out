package tech.pardus.newdesign.read;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.redis.read.RedisSingleValueReader;

/** Redis single values ({@code v:*} / {@code v:n:*}) then database. */
public final class L2OnlySingleValueStrategy<T> implements SingleValueReadStrategy<T> {

  private final RedisSingleValueReader<T> redisReader;

  public L2OnlySingleValueStrategy(RedisSingleValueReader<T> redisReader) {
    this.redisReader = redisReader;
  }

  @Override
  public ReadTier tier() {
    return ReadTier.L2_ONLY;
  }

  @Override
  public Mono<Optional<T>> findById(String id, Mono<T> databaseFallback) {
    if (id == null || id.isBlank()) {
      return Mono.just(Optional.empty());
    }
    return redisReader.readById(id, databaseFallback).map(Optional::of).defaultIfEmpty(Optional.empty());
  }

  @Override
  public Mono<T> findByName(String alias, Mono<T> databaseFallback) {
    if (alias == null || alias.isBlank()) {
      return Mono.empty();
    }
    return redisReader.readByName(alias, databaseFallback);
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
