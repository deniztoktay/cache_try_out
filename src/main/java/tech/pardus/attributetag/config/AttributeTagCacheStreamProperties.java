package tech.pardus.attributetag.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.attribute-tag.stream")
public class AttributeTagCacheStreamProperties {
  private boolean listenerEnabled = true;
  private Duration retentionTtl = Duration.ofHours(24);
  private Duration pollBlock = Duration.ofSeconds(2);
  private int batchSize = 32;
}
