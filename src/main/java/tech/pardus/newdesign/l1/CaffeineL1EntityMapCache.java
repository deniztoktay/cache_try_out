package tech.pardus.newdesign.l1;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;
import java.util.Optional;
import tech.pardus.newdesign.cachekey.CacheKey;

/** Caffeine-backed id map for one {@link CacheKey} partition. */
public class CaffeineL1EntityMapCache<T> implements L1EntityMapCache<T> {

  private final Cache<String, T> cache;

  public CaffeineL1EntityMapCache(CacheKey cacheKey) {
    var builder = Caffeine.newBuilder();
    if (cacheKey.initialCapacity() > 0) {
      builder.initialCapacity((int) Math.min(cacheKey.initialCapacity(), Integer.MAX_VALUE));
    }
    if (cacheKey.maxCapacity() > 0) {
      builder.maximumSize(cacheKey.maxCapacity());
    }
    this.cache = builder.build();
  }

  @Override
  public Optional<T> getById(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(cache.getIfPresent(id));
  }

  @Override
  public Map<String, T> asMap() {
    return Map.copyOf(cache.asMap());
  }

  @Override
  public void put(String id, T value) {
    if (id != null && !id.isBlank() && value != null) {
      cache.put(id, value);
    }
  }

  @Override
  public void putAll(Map<String, T> entries) {
    if (entries != null) {
      entries.forEach(this::put);
    }
  }

  @Override
  public void remove(String id) {
    if (id != null && !id.isBlank()) {
      cache.invalidate(id);
    }
  }

  @Override
  public void clear() {
    cache.invalidateAll();
  }
}
