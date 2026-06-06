package tech.pardus.newdesign.read;

import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.l1.L1EntityMapCache;
import tech.pardus.newdesign.redis.read.RedisGroupedCollectionReader;

/**
 * L2 grouped collections; L1 id map warmed from grouped query results. Entity ids are shared
 * across tiers ({@link tech.pardus.newdesign.cachekey.CacheEntityId}).
 */
public final class L1L2GroupedCollectionStrategy<T> implements GroupedCollectionReadStrategy<T> {

  private final L1EntityMapCache<T> l1;
  private final RedisGroupedCollectionReader<T> redisReader;
  private final Function<T, String> idExtractor;

  public L1L2GroupedCollectionStrategy(
      L1EntityMapCache<T> l1,
      RedisGroupedCollectionReader<T> redisReader,
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
  public Mono<List<T>> findByValueGroup(String groupId, Mono<List<T>> databaseFallback) {
    if (groupId == null || groupId.isBlank()) {
      return Mono.just(List.of());
    }
    return redisReader.readByValueGroup(groupId, databaseFallback).doOnNext(this::indexIntoL1);
  }

  @Override
  public Mono<List<T>> findByNamedGroup(String groupAlias, Mono<List<T>> databaseFallback) {
    if (groupAlias == null || groupAlias.isBlank()) {
      return Mono.just(List.of());
    }
    return redisReader.readByNamedGroup(groupAlias, databaseFallback).doOnNext(this::indexIntoL1);
  }

  public Mono<Void> warmL1(Mono<List<T>> allFromDatabase) {
    return allFromDatabase.doOnNext(this::indexIntoL1).then();
  }

  private void indexIntoL1(List<T> values) {
    if (values == null || values.isEmpty()) {
      return;
    }
    for (T value : values) {
      var id = idExtractor.apply(value);
      if (id != null && !id.isBlank()) {
        l1.put(id, value);
      }
    }
  }
}
