package tech.pardus.cache.l1;

import org.springframework.stereotype.Component;

@Component
public class ResizableL1CacheFactory {
  private final L1CacheProperties properties;

  public ResizableL1CacheFactory(L1CacheProperties properties) {
    this.properties = properties;
  }

  public <K, V> ResizableL1Cache<K, V> create(String name, L1ResizePersister<K, V> resizePersister) {
    return new ResizableL1Cache<>(name, properties, resizePersister);
  }
}
