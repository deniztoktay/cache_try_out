package tech.pardus.format.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.cache.l2.L2CacheOperations;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.format.cache.FormatTypeCacheValueCodec;
import tech.pardus.format.cache.FormatTypeEntityLoader;
import tech.pardus.format.mapper.FormatTypeMapper;
import tech.pardus.format.model.FormatType;
import tech.pardus.format.model.FormatTypeKeys;
import tech.pardus.format.model.FormatTypeModel;
import tech.pardus.jdbc.format.repository.FormatTypeJpaRepository;
import tech.pardus.redis.api.CacheInitializer;
import tech.pardus.redis.coordination.CacheLeaderCoordinator;
import tech.pardus.redis.support.TtlPolicy;

@Slf4j
@Service
public class FormatTypeService {

  private final FormatTypeEntityLoader entityLoader;
  private final FormatTypeJpaRepository jpaRepository;
  private final CacheInitializer cacheInitializer;
  private final FormatTypeCacheValueCodec codec;
  private final FormatTypeMapper mapper;
  private final CacheEntityDescriptor descriptor;
  private final CacheReadStrategy<Integer, FormatTypeModel> readStrategy;
  private final L2CacheOperations<FormatTypeModel> l2Operations;
  private final CacheLeaderCoordinator leaderCoordinator;

  public FormatTypeService(
      FormatTypeEntityLoader entityLoader,
      FormatTypeJpaRepository jpaRepository,
      CacheInitializer cacheInitializer,
      FormatTypeCacheValueCodec codec,
      FormatTypeMapper mapper,
      @Qualifier("formatTypeCacheDescriptor") CacheEntityDescriptor formatTypeCacheDescriptor,
      @Qualifier("formatTypeCacheReadStrategy")
          CacheReadStrategy<Integer, FormatTypeModel> formatTypeCacheReadStrategy,
      @Qualifier("formatTypeL2CacheOperations")
          L2CacheOperations<FormatTypeModel> formatTypeL2CacheOperations,
      CacheLeaderCoordinator leaderCoordinator) {
    this.entityLoader = entityLoader;
    this.jpaRepository = jpaRepository;
    this.cacheInitializer = cacheInitializer;
    this.codec = codec;
    this.mapper = mapper;
    this.descriptor = formatTypeCacheDescriptor;
    this.readStrategy = formatTypeCacheReadStrategy;
    this.l2Operations = formatTypeL2CacheOperations;
    this.leaderCoordinator = leaderCoordinator;
  }

  public Mono<FormatType> getById(Integer id) {
    if (id == null) {
      return Mono.empty();
    }
    return readStrategy
        .getByMemberId(String.valueOf(id), entityLoader.findById(id))
        .flatMap(this::modelToDomainWithTimestamps);
  }

  public Mono<FormatType> getByFormatValueAndCulture(String formatValue, String culture) {
    if (formatValue == null || formatValue.isBlank()) {
      return Mono.empty();
    }
    var aliasId = FormatTypeKeys.valueCultureAliasMemberId(formatValue, culture);
    return readStrategy
        .getByMemberId(
            aliasId, entityLoader.findByFormatValueAndCulture(formatValue, culture))
        .flatMap(this::modelToDomainWithTimestamps);
  }

  public Flux<FormatType> findAll() {
    return readStrategy
        .getAllIndexed(entityLoader.findAll())
        .flatMapMany(
            models -> Flux.fromIterable(models).concatMap(this::modelToDomainWithTimestamps));
  }

  /** Leader populates Redis L2 from DB; no L1 on this tier. */
  public Mono<Void> startupCacheCoordination() {
    TtlPolicy.requirePositive(descriptor.ttl(), "formatTypeCache");
    return leaderCoordinator.runStartup(
        descriptor.namespace(), descriptor.ttl(), populateL2FromDatabase(), Mono.empty());
  }

  public Mono<Void> populateL2FromDatabase() {
    log.info("Populating L2 for format-type namespace={}", descriptor.namespace().name());
    return entityLoader
        .findAll()
        .flatMap(
            models ->
                cacheInitializer
                    .initialize(descriptor.namespace(), descriptor.ttl(), Mono.just(models), codec)
                    .then(writeAllAliases(models)));
  }

  private Mono<Void> writeAllAliases(List<FormatTypeModel> models) {
    if (l2Operations instanceof tech.pardus.cache.l2.ReactiveL2CacheOperations<FormatTypeModel> reactive) {
      return reactive.writeAllAliases(models);
    }
    return Mono.empty();
  }

  private Mono<FormatType> modelToDomainWithTimestamps(FormatTypeModel model) {
    if (model.id() == null) {
      return Mono.just(mapper.toDomain(model));
    }
    return Mono.fromCallable(() -> jpaRepository.findById(model.id()))
        .flatMap(
            opt ->
                opt.map(entity -> Mono.just(mapper.toDomain(entity)))
                    .orElseGet(() -> Mono.just(mapper.toDomain(model))));
  }
}
