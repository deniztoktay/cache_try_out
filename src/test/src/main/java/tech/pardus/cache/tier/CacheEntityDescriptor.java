package tech.pardus.cache.tier;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import tech.pardus.redis.cache.CacheNamespace;

/** Metadata for wiring tier-specific view/save and cache infrastructure. */
public record CacheEntityDescriptor(
    String entityName,
    CacheServiceTier tier,
    CacheNamespace namespace,
    Duration ttl,
    Optional<StreamConfig> streamConfig) {

  public CacheEntityDescriptor {
    Objects.requireNonNull(entityName, "entityName");
    Objects.requireNonNull(tier, "tier");
    Objects.requireNonNull(namespace, "namespace");
    Objects.requireNonNull(ttl, "ttl");
    streamConfig = streamConfig == null ? Optional.empty() : streamConfig;
    if (tier == CacheServiceTier.L1_L2 && streamConfig.isEmpty()) {
      throw new IllegalArgumentException("L1_L2 entity requires streamConfig");
    }
    if (tier != CacheServiceTier.L1_L2 && streamConfig.isPresent()) {
      throw new IllegalArgumentException("streamConfig only applies to L1_L2 tier");
    }
  }

  public static CacheEntityDescriptor dbOnly(String entityName, CacheNamespace namespace, Duration ttl) {
    return new CacheEntityDescriptor(
        entityName, CacheServiceTier.DB_ONLY, namespace, ttl, Optional.empty());
  }

  public static CacheEntityDescriptor l2Only(String entityName, CacheNamespace namespace, Duration ttl) {
    return new CacheEntityDescriptor(
        entityName, CacheServiceTier.L2_ONLY, namespace, ttl, Optional.empty());
  }

  public static CacheEntityDescriptor l1L2(
      String entityName, CacheNamespace namespace, Duration ttl, StreamConfig stream) {
    return new CacheEntityDescriptor(
        entityName, CacheServiceTier.L1_L2, namespace, ttl, Optional.of(stream));
  }

  public boolean requiresL1() {
    return tier == CacheServiceTier.L1_L2;
  }

  public boolean requiresL2() {
    return tier == CacheServiceTier.L2_ONLY || tier == CacheServiceTier.L1_L2;
  }
}
