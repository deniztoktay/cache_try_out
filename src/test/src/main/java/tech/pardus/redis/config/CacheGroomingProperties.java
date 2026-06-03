package tech.pardus.redis.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.grooming")
public class CacheGroomingProperties {
  private boolean enabled = true;
  private long intervalMs = 300_000L;
  private Duration indexTtl = Duration.ofHours(24);
  private List<String> namespaces = new ArrayList<>();
}
