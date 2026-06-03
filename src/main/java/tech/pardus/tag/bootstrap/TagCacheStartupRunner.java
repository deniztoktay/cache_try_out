package tech.pardus.tag.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tech.pardus.redis.bootstrap.CacheStartupRunnerSupport;
import tech.pardus.redis.config.CacheCoordinationProperties;
import tech.pardus.tag.service.TagService;

/** Coordinates multi-pod cache startup: one leader for L2, all pods warm L1 after L2 is ready. */
@Slf4j
@Component
@Order(100)
@ConditionalOnProperty(prefix = "pardus.cache.startup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TagCacheStartupRunner implements ApplicationRunner {
  private final TagService tagService;
  private final CacheCoordinationProperties coordinationProperties;

  public TagCacheStartupRunner(
      TagService tagService, CacheCoordinationProperties coordinationProperties) {
    this.tagService = tagService;
    this.coordinationProperties = coordinationProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    CacheStartupRunnerSupport.runBestEffort(
        "Tag",
        tagService.startupCacheCoordination(),
        coordinationProperties.getStartupTimeout());
  }
}
