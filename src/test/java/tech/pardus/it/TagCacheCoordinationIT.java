package tech.pardus.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.test.StepVerifier;
import tech.pardus.it.support.AbstractLocalDockerIT;
import tech.pardus.it.support.LocalDockerAssumptions;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.coordination.CacheL2ReadyMarker;
import tech.pardus.tag.config.TagCacheProperties;
import tech.pardus.r2dbc.tag.repository.TagR2dbcRepository;
import tech.pardus.tag.service.TagService;

/**
 * End-to-end cache coordination against local Redis + SQL Server (leader L2 populate, L1 warm-up).
 */
class TagCacheCoordinationIT extends AbstractLocalDockerIT {

  @Autowired private ReactiveStringRedisTemplate redis;
  @Autowired private R2dbcEntityTemplate r2dbc;
  @Autowired private TagService tagService;
  @Autowired private TagR2dbcRepository tagRepository;
  @Autowired private TagCacheProperties tagCacheProperties;
  @Autowired private CacheL2ReadyMarker readyMarker;

  @BeforeEach
  void requireInfrastructure() {
    LocalDockerAssumptions.assumeRedisAvailable(redis);
    LocalDockerAssumptions.assumeTagTableReadable(r2dbc);
  }

  @Test
  void startupCacheCoordination_populatesRedisAndMarksReady() {
    var namespace = new CacheNamespace(tagCacheProperties.getNamespace());

    StepVerifier.create(tagService.startupCacheCoordination()).verifyComplete();

    StepVerifier.create(readyMarker.isReady(namespace)).expectNext(true).verifyComplete();

    StepVerifier.create(
            redis
                .hasKey(CacheKeyLayout.liveIndexKey(namespace))
                .zipWith(redis.hasKey(CacheKeyLayout.l2ReadyKey(namespace))))
        .expectNextMatches(tuple -> Boolean.TRUE.equals(tuple.getT1()) && Boolean.TRUE.equals(tuple.getT2()))
        .verifyComplete();
  }

  @Test
  void getById_afterCoordination_returnsTagFromCacheTiers() {
    StepVerifier.create(tagService.startupCacheCoordination()).verifyComplete();

    StepVerifier.create(
            tagRepository
                .findAll()
                .next()
                .flatMap(record -> tagService.getById(record.id())))
        .expectNextMatches(tag -> tag.id() != null)
        .verifyComplete();
  }
}
