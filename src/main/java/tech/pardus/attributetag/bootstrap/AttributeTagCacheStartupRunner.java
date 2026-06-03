package tech.pardus.attributetag.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tech.pardus.attributetag.service.AttributeTagService;
import tech.pardus.redis.bootstrap.CacheStartupRunnerSupport;
import tech.pardus.redis.config.CacheCoordinationProperties;

@Slf4j
@Component
@Order(102)
@ConditionalOnProperty(prefix = "pardus.cache.startup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AttributeTagCacheStartupRunner implements ApplicationRunner {

  private final AttributeTagService attributeTagService;
  private final CacheCoordinationProperties coordinationProperties;

  public AttributeTagCacheStartupRunner(
      AttributeTagService attributeTagService, CacheCoordinationProperties coordinationProperties) {
    this.attributeTagService = attributeTagService;
    this.coordinationProperties = coordinationProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    CacheStartupRunnerSupport.runBestEffort(
        "AttributeTag",
        attributeTagService.startupCacheCoordination(),
        coordinationProperties.getStartupTimeout());
  }
}
