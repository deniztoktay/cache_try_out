package tech.pardus.newdesign.cachekey;

import java.time.Duration;
import tech.pardus.newdesign.read.ReadTier;

/**
 * Metadata for one cache partition. Each implementation is a Spring {@code @Component}; resolve
 * instances with {@link CacheKeyFactory#get(ApplicationCache)} using {@link #cache()}.
 */
public interface CacheKey {

  String getSiteName();

  /** Logical cache name within the site (for example {@code tag}). */
  String getKey();

  /** Registry id for factory lookup. */
  ApplicationCache cache();

  /** L1 initial entry count ({@code 0} when this partition has no L1). */
  long initialCapacity();

  /** L1 maximum entry count ({@code 0} when this partition has no L1). */
  long maxCapacity();

  /** When {@code true}, Redis payloads use the shorter {@link #ttl()}. */
  boolean isTemporary();

  ReadTier readTier();

  /** Root Redis/L1 namespace for this partition. */
  default String getParentKey() {
    return getSiteName() + ":cache:" + getKey();
  }

  default Duration ttl() {
    return isTemporary() ? Duration.ofMinutes(15) : Duration.ofMinutes(150);
  }

  default boolean supportsL1() {
    return readTier() == ReadTier.L1_L2 && maxCapacity() > 0;
  }
}
