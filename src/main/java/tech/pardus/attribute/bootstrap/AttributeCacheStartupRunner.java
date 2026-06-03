package tech.pardus.attribute.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tech.pardus.attribute.service.AttributeService;
import tech.pardus.redis.bootstrap.CacheStartupRunnerSupport;
import tech.pardus.redis.config.CacheCoordinationProperties;

@Slf4j
@Component
@Order(101)
@ConditionalOnProperty(prefix = "pardus.cache.startup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AttributeCacheStartupRunner implements ApplicationRunner {

  private final AttributeService attributeService;
  private final CacheCoordinationProperties coordinationProperties;

  public AttributeCacheStartupRunner(
      AttributeService attributeService, CacheCoordinationProperties coordinationProperties) {
    this.attributeService = attributeService;
    this.coordinationProperties = coordinationProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    CacheStartupRunnerSupport.runBestEffort(
        "Attribute",
        attributeService.startupCacheCoordination(),
        coordinationProperties.getStartupTimeout());
  }
}
