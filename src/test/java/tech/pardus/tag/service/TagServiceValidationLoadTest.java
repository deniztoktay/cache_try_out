package tech.pardus.tag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.cache.l2.L2SingleValueLoader;
import tech.pardus.cache.read.L1L2CacheReadStrategy;
import tech.pardus.r2dbc.tag.entity.TagRecord;
import tech.pardus.r2dbc.tag.repository.TagR2dbcRepository;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.coordination.CacheL2ReadyMarker;
import tech.pardus.tag.cache.TagEntityLoader;
import tech.pardus.tag.mapper.TagMapper;
import tech.pardus.tag.model.TagModel;

class TagServiceValidationLoadTest {

  private TagEntityLoader entityLoader;
  private TagR2dbcRepository repository;
  private TagMapper tagMapper;
  private TieredReactiveCacheReader<TagModel> tieredReader;
  private ResizableL1Cache<String, TagModel> l1Cache;
  private CacheL2ReadyMarker l2ReadyMarker;
  private L1L2CacheReadStrategy<Integer, TagModel> strategy;

  @BeforeEach
  void setUp() {
    entityLoader = mock(TagEntityLoader.class);
    repository = mock(TagR2dbcRepository.class);
    tagMapper = mock(TagMapper.class);
    tieredReader = mock(TieredReactiveCacheReader.class);
    l1Cache = mock(ResizableL1Cache.class);
    l2ReadyMarker = mock(CacheL2ReadyMarker.class);
    var l2Loader = mock(L2SingleValueLoader.class);

    strategy =
        new L1L2CacheReadStrategy<>(
            tieredReader,
            l1Cache,
            l2Loader,
            entityLoader,
            l2ReadyMarker,
            new CacheNamespace("tags-test"));
  }

  @Test
  void loadAllForValidation_whenLeaderInitializing_usesDatabaseOnly() {
    when(l2ReadyMarker.isReady(any())).thenReturn(Mono.just(false));
    var record = new TagRecord(1, "A", "T1", "SYSTEM", false);
    var model = new TagModel(1, "A", "T1", "SYSTEM", false);
    when(entityLoader.findAll()).thenReturn(Mono.just(List.of(model)));

    StepVerifier.create(strategy.loadAllForValidation())
        .assertNext(list -> assertEquals(List.of(model), list))
        .verifyComplete();

    verify(tieredReader, never()).tryGetAllFromL1();
  }
}
