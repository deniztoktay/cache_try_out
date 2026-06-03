package tech.pardus.cache.tier;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;

/** Change-stream settings for {@link CacheServiceTier#L1_L2} entities. */
@Getter
@Setter
public class StreamConfig {
  private boolean listenerEnabled = true;
  private Duration retentionTtl = Duration.ofHours(24);
  private Duration pollBlock = Duration.ofSeconds(2);
  private int batchSize = 32;
}
