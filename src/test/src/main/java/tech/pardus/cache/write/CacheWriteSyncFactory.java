package tech.pardus.cache.write;

import tech.pardus.cache.l2.L2CacheOperations;
import tech.pardus.cache.projection.CacheEventPublisher;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.redis.cache.Identifiable;

public final class CacheWriteSyncFactory {

  private CacheWriteSyncFactory() {}

  public static <ID, M extends Identifiable<ID>> CacheWriteSync<ID, M> create(
      CacheEntityDescriptor descriptor,
      L2CacheOperations<M> l2Operations,
      CacheEventPublisher eventPublisher) {

    return switch (descriptor.tier()) {
      case DB_ONLY -> new NoOpCacheWriteSync<>();
      case L2_ONLY -> {
        if (l2Operations == null) {
          throw new IllegalStateException("L2_ONLY tier requires L2CacheOperations");
        }
        yield new L2OnlyCacheWriteSync<>(l2Operations);
      }
      case L1_L2 -> {
        if (l2Operations == null || eventPublisher == null) {
          throw new IllegalStateException("L1_L2 tier requires L2CacheOperations and CacheEventPublisher");
        }
        yield new L1L2CacheWriteSync<>(l2Operations, eventPublisher);
      }
    };
  }
}
