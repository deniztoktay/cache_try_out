package tech.pardus.cache.l1;

import java.util.Map;

/**
 * Called with a snapshot of the L1 cache before it is resized so entries can be persisted to L2
 * (Redis) before capacity is increased.
 */
@FunctionalInterface
public interface L1ResizePersister<K, V> {

  void persistBeforeResize(Map<K, V> snapshot);
}
