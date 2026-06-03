package tech.pardus.cache.read;

import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.cache.l2.L2SingleValueLoader;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.cache.tier.CacheServiceTier;
import tech.pardus.redis.cache.Identifiable;
import tech.pardus.redis.cache.ReactiveCacheReader;
import tech.pardus.redis.coordination.CacheL2ReadyMarker;

public final class CacheReadStrategyFactory {

  private CacheReadStrategyFactory() {}

  public static <ID, M extends Identifiable<ID>> CacheReadStrategy<ID, M> create(
      CacheEntityDescriptor descriptor,
      EntityLoader<ID, M> loader,
      CacheReadStrategyDependencies<ID, M> dependencies) {

    return switch (descriptor.tier()) {
      case DB_ONLY -> new DbOnlyCacheReadStrategy<>(loader);
      case L2_ONLY ->
          new L2OnlyCacheReadStrategy<>(requireL2Reader(dependencies), loader);
      case L1_L2 ->
          new L1L2CacheReadStrategy<>(
              requireTieredReader(dependencies),
              requireL1Cache(dependencies),
              requireL2Loader(dependencies, descriptor),
              loader,
              requireReadyMarker(dependencies),
              descriptor.namespace());
    };
  }

  private static <ID, M extends Identifiable<ID>> ReactiveCacheReader<M> requireL2Reader(
      CacheReadStrategyDependencies<ID, M> deps) {
    if (deps.l2Reader() == null) {
      throw new IllegalStateException("L2_ONLY/L1_L2 tier requires l2Reader");
    }
    return deps.l2Reader();
  }

  private static <ID, M extends Identifiable<ID>> TieredReactiveCacheReader<M> requireTieredReader(
      CacheReadStrategyDependencies<ID, M> deps) {
    if (deps.tieredReader() == null) {
      throw new IllegalStateException("L1_L2 tier requires tieredReader");
    }
    return deps.tieredReader();
  }

  private static <ID, M extends Identifiable<ID>> ResizableL1Cache<String, M> requireL1Cache(
      CacheReadStrategyDependencies<ID, M> deps) {
    if (deps.l1Cache() == null) {
      throw new IllegalStateException("L1_L2 tier requires l1Cache");
    }
    return deps.l1Cache();
  }

  private static <ID, M extends Identifiable<ID>> L2SingleValueLoader<M> requireL2Loader(
      CacheReadStrategyDependencies<ID, M> deps, CacheEntityDescriptor descriptor) {
    if (deps.l2Loader() != null) {
      return deps.l2Loader();
    }
    if (descriptor.tier() == CacheServiceTier.L1_L2 && deps.valueStore() != null) {
      return new L2SingleValueLoader<>(
          descriptor.namespace(),
          deps.valueStore(),
          deps.indexStore(),
          deps.codec());
    }
    throw new IllegalStateException("L1_L2 tier requires l2Loader or value/index/codec beans");
  }

  private static <ID, M extends Identifiable<ID>> CacheL2ReadyMarker requireReadyMarker(
      CacheReadStrategyDependencies<ID, M> deps) {
    if (deps.l2ReadyMarker() == null) {
      throw new IllegalStateException("L1_L2 tier requires l2ReadyMarker");
    }
    return deps.l2ReadyMarker();
  }
}
