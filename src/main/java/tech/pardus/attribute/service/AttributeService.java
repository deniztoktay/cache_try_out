package tech.pardus.attribute.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.attribute.cache.AttributeEntityLoader;
import tech.pardus.attribute.config.AttributeCacheProperties;
import tech.pardus.attribute.mapper.AttributeMapper;
import tech.pardus.attribute.model.Attribute;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.cache.l2.L2CacheOperations;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.cache.read.L1L2CacheReadStrategy;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.r2dbc.attribute.repository.AttributeR2dbcRepository;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.CacheInitializer;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.coordination.CacheLeaderCoordinator;
import tech.pardus.redis.support.RedisErrors;
import tech.pardus.redis.support.TtlPolicy;
import tech.pardus.attribute.cache.AttributeCacheValueCodec;

@Slf4j
@Service
public class AttributeService {

  private final AttributeEntityLoader entityLoader;
  private final AttributeR2dbcRepository repository;
  private final CacheInitializer cacheInitializer;
  private final CacheIndexStore indexStore;
  private final RedisValueStore valueStore;
  private final AttributeCacheValueCodec codec;
  private final AttributeMapper attributeMapper;
  private final CacheEntityDescriptor descriptor;
  private final CacheReadStrategy<Integer, AttributeModel> readStrategy;
  private final L2CacheOperations<AttributeModel> l2Operations;
  private final CacheLeaderCoordinator leaderCoordinator;

  public AttributeService(
      AttributeEntityLoader entityLoader,
      AttributeR2dbcRepository repository,
      CacheInitializer cacheInitializer,
      CacheIndexStore indexStore,
      RedisValueStore valueStore,
      AttributeCacheValueCodec codec,
      AttributeMapper attributeMapper,
      @Qualifier("attributeCacheDescriptor") CacheEntityDescriptor attributeCacheDescriptor,
      @Qualifier("attributeCacheReadStrategy")
          CacheReadStrategy<Integer, AttributeModel> attributeCacheReadStrategy,
      @Qualifier("attributeL2CacheOperations") L2CacheOperations<AttributeModel> attributeL2CacheOperations,
      CacheLeaderCoordinator leaderCoordinator) {
    this.entityLoader = entityLoader;
    this.repository = repository;
    this.cacheInitializer = cacheInitializer;
    this.indexStore = indexStore;
    this.valueStore = valueStore;
    this.codec = codec;
    this.attributeMapper = attributeMapper;
    this.descriptor = attributeCacheDescriptor;
    this.readStrategy = attributeCacheReadStrategy;
    this.l2Operations = attributeL2CacheOperations;
    this.leaderCoordinator = leaderCoordinator;
  }

  public Mono<Attribute> getById(Integer id) {
    if (id == null) {
      return Mono.empty();
    }
    return readStrategy
        .getByMemberId(String.valueOf(id), entityLoader.findById(id))
        .flatMap(this::modelToDomainWithTimestamps);
  }

  public Mono<Attribute> getByName(String name) {
    if (name == null || name.isBlank()) {
      return Mono.empty();
    }
    var aliasId = CacheKeyLayout.nameAliasMemberId(name);
    return readStrategy
        .getByMemberId(aliasId, repository.findByName(name).map(attributeMapper::toModel))
        .flatMap(this::modelToDomainWithTimestamps);
  }

  public Flux<Attribute> findAll() {
    return readStrategy
        .getAllIndexed(entityLoader.findAll())
        .flatMapMany(
            models ->
                Flux.fromIterable(models).concatMap(this::modelToDomainWithTimestamps));
  }

  public Mono<Void> startupCacheCoordination() {
    TtlPolicy.requirePositive(descriptor.ttl(), "attributeCache");
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

  private Mono<Attribute> modelToDomainWithTimestamps(AttributeModel model) {
    if (model.id() == null) {
      return Mono.just(attributeMapper.toDomain(model));
    }
    return repository
        .findById(model.id())
        .map(attributeMapper::toDomain)
        .defaultIfEmpty(attributeMapper.toDomain(model));
  }

  private TieredReactiveCacheReader<AttributeModel> tieredReader() {
    return ((L1L2CacheReadStrategy<Integer, AttributeModel>) readStrategy).tieredReader();
  }

  private ResizableL1Cache<String, AttributeModel> l1Cache() {
    return ((L1L2CacheReadStrategy<Integer, AttributeModel>) readStrategy).l1Cache();
  }

  private Mono<Void> writeAllAliases(List<AttributeModel> models) {
    if (l2Operations instanceof tech.pardus.cache.l2.ReactiveL2CacheOperations<AttributeModel> reactive) {
      return reactive.writeAllAliases(models);
    }
    return Mono.empty();
  }

  private Mono<List<AttributeModel>> loadAllModelsFromRedis() {
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
