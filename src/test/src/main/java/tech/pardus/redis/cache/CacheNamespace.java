package tech.pardus.redis.cache;

/**
 * Logical cache partition for a service. Keys are derived via {@link CacheKeyLayout}.
 */
public record CacheNamespace(String name) {

  public CacheNamespace {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Cache namespace name must not be blank");
    }
  }
}
