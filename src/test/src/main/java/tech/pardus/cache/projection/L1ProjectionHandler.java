package tech.pardus.cache.projection;

/** Applies change-stream events to local L1 on {@link CacheServiceTier#L1_L2} entities. */
@FunctionalInterface
public interface L1ProjectionHandler {

  void onChange(CacheChangeStreamMessage message);
}
