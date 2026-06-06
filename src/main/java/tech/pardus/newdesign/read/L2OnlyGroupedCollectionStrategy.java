package tech.pardus.newdesign.read;

import java.util.List;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.redis.read.RedisGroupedCollectionReader;

/** Redis grouped collections ({@code v:*} / {@code v:n:*}) then database. */
public final class L2OnlyGroupedCollectionStrategy<T> implements GroupedCollectionReadStrategy<T> {

  private final RedisGroupedCollectionReader<T> redisReader;

  public L2OnlyGroupedCollectionStrategy(RedisGroupedCollectionReader<T> redisReader) {
    this.redisReader = redisReader;
  }

  @Override
  public ReadTier tier() {
    return ReadTier.L2_ONLY;
  }

  @Override
  public Mono<List<T>> findByValueGroup(String groupId, Mono<List<T>> databaseFallback) {
    if (groupId == null || groupId.isBlank()) {
      return Mono.just(List.of());
    }
    return redisReader.readByValueGroup(groupId, databaseFallback);
  }

  @Override
  public Mono<List<T>> findByNamedGroup(String groupAlias, Mono<List<T>> databaseFallback) {
    if (groupAlias == null || groupAlias.isBlank()) {
      return Mono.just(List.of());
    }
    return redisReader.readByNamedGroup(groupAlias, databaseFallback);
  }
}
