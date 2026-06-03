package tech.pardus.attributetag.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.attribute-tag")
public class AttributeTagCacheProperties {
  private String namespace = "attribute-tag-cache";
  private Duration ttl = Duration.ofHours(6);
}
