package tech.pardus.attribute.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.attribute.cache.AttributeCacheValueCodec;
import tech.pardus.attribute.cache.AttributeEntityLoader;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.cache.l2.L2AliasKeyExtractor;
import tech.pardus.cache.l2.L2CacheOperations;
import tech.pardus.cache.l2.ReactiveL2CacheOperations;
import tech.pardus.cache.projection.CacheEventPublisher;
import tech.pardus.cache.projection.CacheStreamListener;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.cache.read.CacheReadStrategyDependencies;
import tech.pardus.cache.read.CacheReadStrategyFactory;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.cache.tier.StreamConfig;
import tech.pardus.cache.write.CacheWriteSync;
import tech.pardus.cache.write.CacheWriteSyncFactory;
import tech.pardus.jdbc.attribute.view.AttributeL1ProjectionHandler;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisKeyMaintenance;
import tech.pardus.redis.api.RedisStreamBus;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.ReactiveCacheReader;
import tech.pardus.redis.coordination.CacheL2ReadyMarker;
import tech.pardus.redis.runtime.RedisRuntimeGate;

@Configuration
public class AttributeCacheInfrastructureConfiguration {

  @Bean
  CacheEntityDescriptor attributeCacheDescriptor(
      AttributeCacheProperties cacheProperties, AttributeCacheStreamProperties streamProperties) {
    var stream = new StreamConfig();
    stream.setListenerEnabled(streamProperties.isListenerEnabled());
    stream.setRetentionTtl(streamProperties.getRetentionTtl());
    stream.setPollBlock(streamProperties.getPollBlock());
    stream.setBatchSize(streamProperties.getBatchSize());
    return CacheEntityDescriptor.l1L2(
        "attribute",
        new CacheNamespace(cacheProperties.getNamespace()),
        cacheProperties.getTtl(),
        stream);
  }

  @Bean
  L2CacheOperations<AttributeModel> attributeL2CacheOperations(
      CacheEntityDescriptor attributeCacheDescriptor,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      RedisKeyMaintenance keyMaintenance,
      AttributeCacheValueCodec codec) {
    L2AliasKeyExtractor<AttributeModel> alias =
        model ->
            model.name() != null && !model.name().isBlank()
                ? java.util.Optional.of(model.nameAliasStringId())
                : java.util.Optional.empty();
    return new ReactiveL2CacheOperations<>(
        attributeCacheDescriptor.namespace(),
        attributeCacheDescriptor.ttl(),
        valueStore,
        indexStore,
        keyMaintenance,
        codec,
        alias);
  }

  @Bean
  CacheReadStrategy<Integer, AttributeModel> attributeCacheReadStrategy(
      CacheEntityDescriptor attributeCacheDescriptor,
      AttributeEntityLoader attributeEntityLoader,
      ReactiveCacheReader<AttributeModel> attributeReactiveCacheReader,
      TieredReactiveCacheReader<AttributeModel> attributeTieredCacheReader,
      ResizableL1Cache<String, AttributeModel> attributeL1Cache,
      CacheL2ReadyMarker l2ReadyMarker,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      AttributeCacheValueCodec codec) {

    var dependencies =
        CacheReadStrategyDependencies.<Integer, AttributeModel>builder()
            .l2Reader(attributeReactiveCacheReader)
            .tieredReader(attributeTieredCacheReader)
            .l1Cache(attributeL1Cache)
            .l2ReadyMarker(l2ReadyMarker)
            .valueStore(valueStore)
            .indexStore(indexStore)
            .codec(codec)
            .build();

    return CacheReadStrategyFactory.create(
        attributeCacheDescriptor, attributeEntityLoader, dependencies);
  }

  @Bean
  CacheEventPublisher attributeCacheEventPublisher(
      RedisStreamBus streamBus, CacheEntityDescriptor attributeCacheDescriptor) {
    return new CacheEventPublisher(streamBus, attributeCacheDescriptor);
  }

  @Bean
  CacheWriteSync<Integer, AttributeModel> attributeCacheWriteSync(
      CacheEntityDescriptor attributeCacheDescriptor,
      L2CacheOperations<AttributeModel> attributeL2CacheOperations,
      CacheEventPublisher attributeCacheEventPublisher) {
    return CacheWriteSyncFactory.create(
        attributeCacheDescriptor, attributeL2CacheOperations, attributeCacheEventPublisher);
  }

  @Bean
  AttributeL1ProjectionHandler attributeL1ProjectionHandler(
      CacheReadStrategy<Integer, AttributeModel> attributeCacheReadStrategy,
      ResizableL1Cache<String, AttributeModel> attributeL1Cache) {
    return new AttributeL1ProjectionHandler(attributeCacheReadStrategy, attributeL1Cache);
  }

  @Bean
  CacheStreamListener attributeCacheStreamListener(
      RedisStreamBus streamBus,
      CacheEntityDescriptor attributeCacheDescriptor,
      AttributeL1ProjectionHandler attributeL1ProjectionHandler,
      RedisRuntimeGate redisRuntimeGate) {
    return new CacheStreamListener(
        streamBus, attributeCacheDescriptor, attributeL1ProjectionHandler, redisRuntimeGate);
  }
}
