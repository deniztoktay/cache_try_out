package tech.pardus.tag.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.cache.l2.L2CacheOperations;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.cache.read.L1L2CacheReadStrategy;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.r2dbc.tag.repository.TagR2dbcRepository;
import tech.pardus.redis.api.CacheInitializer;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.coordination.CacheLeaderCoordinator;
import tech.pardus.redis.support.RedisErrors;
import tech.pardus.redis.support.TtlPolicy;
import tech.pardus.tag.cache.TagCacheValueCodec;
import tech.pardus.tag.cache.TagEntityLoader;
import tech.pardus.tag.mapper.TagMapper;
import tech.pardus.tag.model.Tag;
import tech.pardus.tag.model.TagModel;

@Slf4j
@Service
public class TagService {

  private final TagEntityLoader entityLoader;
  private final TagR2dbcRepository repository;
  private final CacheInitializer cacheInitializer;
  private final CacheIndexStore indexStore;
  private final RedisValueStore valueStore;
  private final TagCacheValueCodec codec;
  private final TagMapper tagMapper;
  private final CacheEntityDescriptor descriptor;
  private final CacheReadStrategy<Integer, TagModel> readStrategy;
  private final L2CacheOperations<TagModel> l2Operations;
  private final CacheLeaderCoordinator leaderCoordinator;

  public TagService(
      TagEntityLoader entityLoader,
      TagR2dbcRepository repository,
      CacheInitializer cacheInitializer,
      CacheIndexStore indexStore,
      RedisValueStore valueStore,
      TagCacheValueCodec codec,
      TagMapper tagMapper,
      @Qualifier("tagCacheDescriptor") CacheEntityDescriptor tagCacheDescriptor,
      @Qualifier("tagCacheReadStrategy") CacheReadStrategy<Integer, TagModel> tagCacheReadStrategy,
      @Qualifier("tagL2CacheOperations") L2CacheOperations<TagModel> tagL2CacheOperations,
      CacheLeaderCoordinator leaderCoordinator) {
    this.entityLoader = entityLoader;
    this.repository = repository;
    this.cacheInitializer = cacheInitializer;
    this.indexStore = indexStore;
    this.valueStore = valueStore;
    this.codec = codec;
    this.tagMapper = tagMapper;
    this.descriptor = tagCacheDescriptor;
    this.readStrategy = tagCacheReadStrategy;
    this.l2Operations = tagL2CacheOperations;
    this.leaderCoordinator = leaderCoordinator;
  }

  public Mono<Tag> getById(Integer id) {
    if (id == null) {
      return Mono.empty();
    }
    return readStrategy
        .getByMemberId(String.valueOf(id), entityLoader.findById(id))
        .map(tagMapper::toDomain);
  }

  public Mono<Tag> getByName(String name) {
    if (name == null || name.isBlank()) {
      return Mono.empty();
    }
    var aliasId = CacheKeyLayout.nameAliasMemberId(name);
    return readStrategy
        .getByMemberId(aliasId, repository.findByName(name).map(tagMapper::toModel))
        .map(tagMapper::toDomain);
  }

  public Flux<Tag> findAll() {
    return readStrategy
        .getAllIndexed(entityLoader.findAll())
        .flatMapMany(models -> Flux.fromIterable(tagMapper.toDomains(models)));
  }

  public Flux<Tag> findAssignable() {
    return findAll().filter(tag -> Boolean.TRUE.equals(tag.canUserAssign()));
  }

  public Mono<Void> startupCacheCoordination() {
    TtlPolicy.requirePositive(descriptor.ttl(), "tagCache");
    return leaderCoordinator.runStartup(
        descriptor.namespace(), descriptor.ttl(), populateL2FromDatabase(), warmL1FromL2());
  }

  public Mono<Void> populateL2FromDatabase() {
    log.info("Leader populating L2 for namespace={}", descriptor.namespace().name());
    return entityLoader
        .findAll()
        .flatMap(
            models ->
                cacheInitializer
                    .initialize(descriptor.namespace(), descriptor.ttl(), Mono.just(models), codec)
                    .then(writeAllAliases(models)));
  }

  public Mono<Void> warmL1FromL2() {
    log.info("Warming L1 from L2 for namespace={}", descriptor.namespace().name());
    return loadAllModelsFromRedis()
        .doOnNext(
            models -> {
              tieredReader().warmL1(models);
              models.stream()
                  .filter(m -> m.name() != null && !m.name().isBlank())
                  .forEach(m -> l1Cache().put(m.nameAliasStringId(), m));
              log.info(
                  "L1 warm complete: entries={}, maxSize={}",
                  l1Cache().estimatedSize(),
                  l1Cache().currentMaxSize());
            })
        .then();
  }

  private TieredReactiveCacheReader<TagModel> tieredReader() {
    return ((L1L2CacheReadStrategy<Integer, TagModel>) readStrategy).tieredReader();
  }

  private ResizableL1Cache<String, TagModel> l1Cache() {
    return ((L1L2CacheReadStrategy<Integer, TagModel>) readStrategy).l1Cache();
  }

  private Mono<Void> writeAllAliases(List<TagModel> models) {
    if (l2Operations instanceof tech.pardus.cache.l2.ReactiveL2CacheOperations<TagModel> reactive) {
      return reactive.writeAllAliases(models);
    }
    return Mono.empty();
  }

  private Mono<List<TagModel>> loadAllModelsFromRedis() {
    var indexKey = CacheKeyLayout.liveIndexKey(descriptor.namespace());
    return indexStore
        .listMembers(indexKey)
        .flatMap(
            stringId ->
                valueStore
                    .getBytes(CacheKeyLayout.liveValueKey(descriptor.namespace(), stringId))
                    .map(bytes -> codec.decode(bytes, stringId))
                    .onErrorResume(
                        ex -> RedisErrors.isNoSuchKey(ex) ? Mono.empty() : Mono.error(ex)))
        .collectList()
        .doOnNext(
            models -> {
              if (models.isEmpty()) {
                log.warn(
                    "L2 index empty or stale for namespace {}; L1 warm skipped",
                    descriptor.namespace().name());
              }
            });
  }
}
