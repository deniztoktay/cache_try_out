package tech.pardus.cache.l1;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

/**
 * Caffeine L1 cache that grows when {@link L1CacheProperties#getResizeThreshold()} is reached. A
 * snapshot is persisted via {@link L1ResizePersister} before each resize so L2 is updated first.
 */
@Slf4j
public class ResizableL1Cache<K, V> {
  private final String name;
  private final L1CacheProperties properties;
  private final L1ResizePersister<K, V> resizePersister;
  private final ReentrantLock resizeLock = new ReentrantLock();

  private volatile long currentMaxSize;
  private volatile Cache<K, V> cache;

  public ResizableL1Cache(
      String name, L1CacheProperties properties, L1ResizePersister<K, V> resizePersister) {
    this.name = name;
    this.properties = properties;
    this.resizePersister = resizePersister;
    this.currentMaxSize = Math.max(1L, properties.getInitialMaxSize());
    this.cache = buildCache(currentMaxSize);
  }

  public Optional<V> get(K key) {
    return Optional.ofNullable(cache.getIfPresent(key));
  }

  public void put(K key, V value) {
    if (Objects.isNull(key) || Objects.isNull(value)) {
      return;
    }
    resizeIfAtThreshold();
    cache.put(key, value);
    resizeIfAtThreshold();
  }

  public void putAll(Map<K, V> entries) {
    if (Objects.isNull(entries) || entries.isEmpty()) {
      return;
    }
    for (var entry : entries.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public Map<K, V> snapshot() {
    return Map.copyOf(cache.asMap());
  }

  public long estimatedSize() {
    return cache.estimatedSize();
  }

  public long currentMaxSize() {
    return currentMaxSize;
  }

  public void clear() {
    cache.invalidateAll();
  }

  public void invalidate(K key) {
    if (Objects.nonNull(key)) {
      cache.invalidate(key);
    }
  }

  private void resizeIfAtThreshold() {
    if (!isAtResizeThreshold()) {
      return;
    }
    resizeLock.lock();
    try {
      if (!isAtResizeThreshold()) {
        return;
      }
      if (currentMaxSize >= properties.getMaxMaxSize()) {
        log.warn(
            "L1 cache '{}' at {}% capacity but already at maxMaxSize={}",
            name,
            (int) (properties.getResizeThreshold() * 100),
            properties.getMaxMaxSize());
        return;
      }

      var snapshot = Map.copyOf(cache.asMap());
      log.info(
          "L1 cache '{}' at resize threshold (size={}, max={}), persisting {} entries before resize",
          name,
          cache.estimatedSize(),
          currentMaxSize,
          snapshot.size());

      resizePersister.persistBeforeResize(snapshot);

      long newMaxSize = nextMaxSize();
      var rebuilt = buildCache(newMaxSize);
      rebuilt.putAll(snapshot);
      cache = rebuilt;
      currentMaxSize = newMaxSize;

      log.info("L1 cache '{}' resized to maxSize={}", name, newMaxSize);
    } finally {
      resizeLock.unlock();
    }
  }

  private boolean isAtResizeThreshold() {
    if (currentMaxSize <= 0) {
      return false;
    }
    double ratio = (double) cache.estimatedSize() / (double) currentMaxSize;
    return ratio >= properties.getResizeThreshold();
  }

  private long nextMaxSize() {
    long grown = (long) Math.ceil(currentMaxSize * properties.getGrowthFactor());
    return Math.min(grown, properties.getMaxMaxSize());
  }

  private Cache<K, V> buildCache(long maxSize) {
    return Caffeine.newBuilder().maximumSize(maxSize).build();
  }
}
