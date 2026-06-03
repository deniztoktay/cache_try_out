package tech.pardus.attribute.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.attribute.stream")
public class AttributeCacheStreamProperties {
  private boolean listenerEnabled = true;
  private Duration retentionTtl = Duration.ofHours(24);
  private Duration pollBlock = Duration.ofSeconds(2);
  private int batchSize = 32;
}
