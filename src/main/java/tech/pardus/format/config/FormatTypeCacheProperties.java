package tech.pardus.format.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.format-type")
public class FormatTypeCacheProperties {
  private String namespace = "format-type-cache";
  private Duration ttl = Duration.ofHours(6);
}
