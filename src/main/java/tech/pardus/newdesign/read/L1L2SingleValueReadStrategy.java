package tech.pardus.newdesign.read;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.l1.L1EntityMapCache;
import tech.pardus.newdesign.redis.read.RedisSingleValueReader;

/**
 * L1 id map plus Redis single values at {@code v:{id}} / {@code v:n:{alias}} (Tag-style caches).
 */
public final class L1L2SingleValueReadStrategy<T>
    implements SingleValueReadStrategy<T>, L1EntityReadStrategy<T> {

  private final L1EntityMapCache<T> l1;
  private final RedisSingleValueReader<T> redisReader;
  private final Function<T, String> idExtractor;

  public L1L2SingleValueReadStrategy(
      L1EntityMapCache<T> l1,
      RedisSingleValueReader<T> redisReader,
      Function<T, String> idExtractor) {
    this.l1 = l1;
    this.redisReader = redisReader;
    this.idExtractor = idExtractor;
  }

  @Override
  public ReadTier tier() {
    return ReadTier.L1_L2;
  }

  @Override
  public Mono<Optional<T>> findById(String id, Mono<T> databaseFallback) {
    if (id == null || id.isBlank()) {
      return Mono.just(Optional.empty());
    }
    var cached = l1.getById(id);
    if (cached.isPresent()) {
      return Mono.just(cached);
    }
    return redisReader
        .readById(id, databaseFallback)
        .doOnNext(value -> l1.put(id, value))
        .map(Optional::of)
        .defaultIfEmpty(Optional.empty());
  }

  @Override
  public Mono<T> findByName(String alias, Mono<T> databaseFallback) {
    if (alias == null || alias.isBlank()) {
      return Mono.empty();
    }
    return redisReader
        .readByName(alias, databaseFallback)
        .doOnNext(value -> indexIntoL1(value));
  }

  @Override
  public Mono<List<T>> find(Predicate<T> predicate, Mono<List<T>> databaseFallbackWhenEmpty) {
    if (predicate == null) {
      return Mono.just(List.of());
    }
    if (!l1.asMap().isEmpty()) {
      return Mono.just(l1.find(predicate));
    }
    return databaseFallbackWhenEmpty
        .flatMap(
            all -> {
              indexIntoL1(all);
              return Mono.just(l1.find(predicate));
            });
  }

  public Mono<Void> warmL1(Mono<List<T>> allFromDatabase) {
    return allFromDatabase.doOnNext(this::indexIntoL1).then();
  }

  public L1EntityMapCache<T> l1() {
    return l1;
  }

  private void indexIntoL1(List<T> values) {
    if (values == null || values.isEmpty()) {
      return;
    }
    for (T value : values) {
      indexIntoL1(value);
    }
  }

  private void indexIntoL1(T value) {
    if (value == null) {
      return;
    }
    var id = idExtractor.apply(value);
    if (id != null && !id.isBlank()) {
      l1.put(id, value);
    }
  }
}
