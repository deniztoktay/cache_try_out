package tech.pardus.attribute.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.attribute")
public class AttributeCacheProperties {
  private String namespace = "attribute-cache";
  private Duration ttl = Duration.ofHours(6);
}
