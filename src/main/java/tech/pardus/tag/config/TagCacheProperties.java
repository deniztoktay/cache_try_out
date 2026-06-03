package tech.pardus.tag.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.tag")
public class TagCacheProperties {
  private String namespace = "tag-cache";
  private Duration ttl = Duration.ofHours(6);
}
