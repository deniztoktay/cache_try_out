package tech.pardus.cache.l1;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.l1")
public class L1CacheProperties {
  /** When estimated size / max size reaches this ratio, resize runs (persist first, then grow). */
  private double resizeThreshold = 0.95;

  /** Multiplier applied to current max size when resizing (e.g. 1.5 = +50%). */
  private double growthFactor = 1.5;

  private long initialMaxSize = 1_000L;

  /** Upper bound for automatic growth. */
  private long maxMaxSize = 50_000L;
}
