package tech.pardus.format.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tech.pardus.format.service.FormatTypeService;
import tech.pardus.redis.bootstrap.CacheStartupRunnerSupport;
import tech.pardus.redis.config.CacheCoordinationProperties;

@Slf4j
@Component
@Order(103)
@ConditionalOnProperty(prefix = "pardus.cache.startup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FormatTypeCacheStartupRunner implements ApplicationRunner {

  private final FormatTypeService formatTypeService;
  private final CacheCoordinationProperties coordinationProperties;

  public FormatTypeCacheStartupRunner(
      FormatTypeService formatTypeService, CacheCoordinationProperties coordinationProperties) {
    this.formatTypeService = formatTypeService;
    this.coordinationProperties = coordinationProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    CacheStartupRunnerSupport.runBestEffort(
        "FormatType",
        formatTypeService.startupCacheCoordination(),
        coordinationProperties.getStartupTimeout());
  }
}
