package tech.pardus.reference.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.reference")
public class ReferenceCacheProperties {
  private String namespace = "reference-cache";
  private Duration ttl = Duration.ofHours(6);
}
