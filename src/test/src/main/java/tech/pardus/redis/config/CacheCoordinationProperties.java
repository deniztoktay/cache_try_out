package tech.pardus.redis.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pardus.cache.coordination")
public class CacheCoordinationProperties {
  private Duration initLockTtl = Duration.ofMinutes(15);
  private Duration groomLockTtl = Duration.ofMinutes(5);
  private Duration startupTimeout = Duration.ofMinutes(30);
  private Duration l2ReadyPollInterval = Duration.ofSeconds(2);
  private Duration l2ReadyWaitTimeout = Duration.ofMinutes(25);
}
