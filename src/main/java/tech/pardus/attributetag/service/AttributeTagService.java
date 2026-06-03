package tech.pardus.attributetag.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.attributetag.cache.AttributeTagEntityLoader;
import tech.pardus.attributetag.config.AttributeTagCacheProperties;
import tech.pardus.attributetag.mapper.AttributeTagMapper;
import tech.pardus.attributetag.model.AttributeTag;
import tech.pardus.attributetag.model.AttributeTagKeys;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.cache.l2.L2CacheOperations;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.cache.read.L1L2CacheReadStrategy;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.jdbc.attribute.entity.AttributeTagEntity;
import tech.pardus.jdbc.attribute.entity.compositeid.AttributeTagId;
import tech.pardus.jdbc.attribute.repository.AttributeTagJpaRepository;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.CacheInitializer;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.coordination.CacheLeaderCoordinator;
import tech.pardus.redis.support.RedisErrors;
import tech.pardus.redis.support.TtlPolicy;
import tech.pardus.attributetag.cache.AttributeTagCacheValueCodec;

@Slf4j
@Service
public class AttributeTagService {

  private final AttributeTagEntityLoader entityLoader;
  private final AttributeTagJpaRepository jpaRepository;
  private final CacheInitializer cacheInitializer;
  private final CacheIndexStore indexStore;
  private final RedisValueStore valueStore;
  private final AttributeTagCacheValueCodec codec;
  private final AttributeTagMapper mapper;
  private final CacheEntityDescriptor descriptor;
  private final CacheReadStrategy<String, AttributeTagModel> readStrategy;
  private final L2CacheOperations<AttributeTagModel> l2Operations;
  private final CacheLeaderCoordinator leaderCoordinator;

  public AttributeTagService(
      AttributeTagEntityLoader entityLoader,
      AttributeTagJpaRepository jpaRepository,
      CacheInitializer cacheInitializer,
      CacheIndexStore indexStore,
      RedisValueStore valueStore,
      AttributeTagCacheValueCodec codec,
      AttributeTagMapper mapper,
      @Qualifier("attributeTagCacheDescriptor") CacheEntityDescriptor attributeTagCacheDescriptor,
      @Qualifier("attributeTagCacheReadStrategy")
          CacheReadStrategy<String, AttributeTagModel> attributeTagCacheReadStrategy,
      @Qualifier("attributeTagL2CacheOperations")
          L2CacheOperations<AttributeTagModel> attributeTagL2CacheOperations,
      CacheLeaderCoordinator leaderCoordinator) {
    this.entityLoader = entityLoader;
    this.jpaRepository = jpaRepository;
    this.cacheInitializer = cacheInitializer;
    this.indexStore = indexStore;
    this.valueStore = valueStore;
    this.codec = codec;
    this.mapper = mapper;
    this.descriptor = attributeTagCacheDescriptor;
    this.readStrategy = attributeTagCacheReadStrategy;
    this.l2Operations = attributeTagL2CacheOperations;
    this.leaderCoordinator = leaderCoordinator;
  }

  public Mono<AttributeTag> getByKey(Integer attributeId, Integer tagId) {
    if (attributeId == null || tagId == null) {
      return Mono.empty();
    }
    var memberId = AttributeTagKeys.memberId(attributeId, tagId);
    return readStrategy
        .getByMemberId(memberId, entityLoader.findById(memberId))
        .flatMap(this::modelToDomainWithTimestamps);
  }

  public Flux<AttributeTag> findByAttributeId(Integer attributeId) {
    if (attributeId == null) {
      return Flux.empty();
    }
    return readStrategy
        .getAllIndexed(entityLoader.findAll())
        .flatMapMany(
            models ->
                Flux.fromIterable(
                    models.stream()
                        .filter(m -> attributeId.equals(m.attributeId()))
                        .toList()))
        .flatMap(this::modelToDomainWithTimestamps);
  }

  public Flux<AttributeTag> findByTagId(Integer tagId) {
    if (tagId == null) {
      return Flux.empty();
    }
    return readStrategy
        .getAllIndexed(entityLoader.findAll())
        .flatMapMany(
            models ->
                Flux.fromIterable(
                    models.stream().filter(m -> tagId.equals(m.tagId())).toList()))
        .flatMap(this::modelToDomainWithTimestamps);
  }

  public Flux<AttributeTag> findAll() {
    return readStrategy
        .getAllIndexed(entityLoader.findAll())
        .flatMapMany(models -> Flux.fromIterable(models).concatMap(this::modelToDomainWithTimestamps));
  }

  public Mono<Boolean> existsForAttribute(Integer attributeId) {
    return entityLoader.findByAttributeId(attributeId).map(list -> !list.isEmpty());
  }

  public Mono<Boolean> existsForTag(Integer tagId) {
    return entityLoader.findByTagId(tagId).map(list -> !list.isEmpty());
  }

  public Mono<Boolean> existsByKey(Integer attributeId, Integer tagId) {
    return entityLoader.existsByKey(attributeId, tagId);
  }

  public Mono<Void> startupCacheCoordination() {
    TtlPolicy.requirePositive(descriptor.ttl(), "attributeTagCache");
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
                    .then());
  }

  public Mono<Void> warmL1FromL2() {
    log.info("Warming L1 from L2 for namespace={}", descriptor.namespace().name());
    return loadAllModelsFromRedis()
        .doOnNext(
            models -> {
              tieredReader().warmL1(models);
              log.info(
                  "L1 warm complete: entries={}, maxSize={}",
                  l1Cache().estimatedSize(),
                  l1Cache().currentMaxSize());
            })
        .then();
  }

  private Mono<AttributeTag> modelToDomainWithTimestamps(AttributeTagModel model) {
    return Mono.fromCallable(
            () ->
                jpaRepository.findById(
                    new AttributeTagId(model.attributeId(), model.tagId())))
        .flatMap(
            opt ->
                opt.map(entity -> Mono.just(mapper.toDomain(entity)))
                    .orElseGet(() -> Mono.just(mapper.toDomain(model))));
  }

  private TieredReactiveCacheReader<AttributeTagModel> tieredReader() {
    return ((L1L2CacheReadStrategy<String, AttributeTagModel>) readStrategy).tieredReader();
  }

  private ResizableL1Cache<String, AttributeTagModel> l1Cache() {
    return ((L1L2CacheReadStrategy<String, AttributeTagModel>) readStrategy).l1Cache();
  }

  private Mono<List<AttributeTagModel>> loadAllModelsFromRedis() {
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
