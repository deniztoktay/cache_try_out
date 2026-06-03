package tech.pardus.attributetag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.attributetag.cache.AttributeTagCacheValueCodec;
import tech.pardus.attributetag.cache.AttributeTagEntityLoader;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
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
import tech.pardus.jdbc.attributetag.view.AttributeTagL1ProjectionHandler;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisKeyMaintenance;
import tech.pardus.redis.api.RedisStreamBus;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.ReactiveCacheReader;
import tech.pardus.redis.coordination.CacheL2ReadyMarker;
import tech.pardus.redis.runtime.RedisRuntimeGate;

@Configuration
public class AttributeTagCacheInfrastructureConfiguration {

  @Bean
  CacheEntityDescriptor attributeTagCacheDescriptor(
      AttributeTagCacheProperties cacheProperties,
      AttributeTagCacheStreamProperties streamProperties) {
    var stream = new StreamConfig();
    stream.setListenerEnabled(streamProperties.isListenerEnabled());
    stream.setRetentionTtl(streamProperties.getRetentionTtl());
    stream.setPollBlock(streamProperties.getPollBlock());
    stream.setBatchSize(streamProperties.getBatchSize());
    return CacheEntityDescriptor.l1L2(
        "attribute-tag",
        new CacheNamespace(cacheProperties.getNamespace()),
        cacheProperties.getTtl(),
        stream);
  }

  @Bean
  L2CacheOperations<AttributeTagModel> attributeTagL2CacheOperations(
      CacheEntityDescriptor attributeTagCacheDescriptor,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      RedisKeyMaintenance keyMaintenance,
      AttributeTagCacheValueCodec codec) {
    return new ReactiveL2CacheOperations<>(
        attributeTagCacheDescriptor.namespace(),
        attributeTagCacheDescriptor.ttl(),
        valueStore,
        indexStore,
        keyMaintenance,
        codec);
  }

  @Bean
  CacheReadStrategy<String, AttributeTagModel> attributeTagCacheReadStrategy(
      CacheEntityDescriptor attributeTagCacheDescriptor,
      AttributeTagEntityLoader attributeTagEntityLoader,
      ReactiveCacheReader<AttributeTagModel> attributeTagReactiveCacheReader,
      TieredReactiveCacheReader<AttributeTagModel> attributeTagTieredCacheReader,
      ResizableL1Cache<String, AttributeTagModel> attributeTagL1Cache,
      CacheL2ReadyMarker l2ReadyMarker,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      AttributeTagCacheValueCodec codec) {

    var dependencies =
        CacheReadStrategyDependencies.<String, AttributeTagModel>builder()
            .l2Reader(attributeTagReactiveCacheReader)
            .tieredReader(attributeTagTieredCacheReader)
            .l1Cache(attributeTagL1Cache)
            .l2ReadyMarker(l2ReadyMarker)
            .valueStore(valueStore)
            .indexStore(indexStore)
            .codec(codec)
            .build();

    return CacheReadStrategyFactory.create(
        attributeTagCacheDescriptor, attributeTagEntityLoader, dependencies);
  }

  @Bean
  CacheEventPublisher attributeTagCacheEventPublisher(
      RedisStreamBus streamBus, CacheEntityDescriptor attributeTagCacheDescriptor) {
    return new CacheEventPublisher(streamBus, attributeTagCacheDescriptor);
  }

  @Bean
  CacheWriteSync<String, AttributeTagModel> attributeTagCacheWriteSync(
      CacheEntityDescriptor attributeTagCacheDescriptor,
      L2CacheOperations<AttributeTagModel> attributeTagL2CacheOperations,
      CacheEventPublisher attributeTagCacheEventPublisher) {
    return CacheWriteSyncFactory.create(
        attributeTagCacheDescriptor, attributeTagL2CacheOperations, attributeTagCacheEventPublisher);
  }

  @Bean
  AttributeTagL1ProjectionHandler attributeTagL1ProjectionHandler(
      CacheReadStrategy<String, AttributeTagModel> attributeTagCacheReadStrategy,
      ResizableL1Cache<String, AttributeTagModel> attributeTagL1Cache) {
    return new AttributeTagL1ProjectionHandler(attributeTagCacheReadStrategy, attributeTagL1Cache);
  }

  @Bean
  CacheStreamListener attributeTagCacheStreamListener(
      RedisStreamBus streamBus,
      CacheEntityDescriptor attributeTagCacheDescriptor,
      AttributeTagL1ProjectionHandler attributeTagL1ProjectionHandler,
      RedisRuntimeGate redisRuntimeGate) {
    return new CacheStreamListener(
        streamBus, attributeTagCacheDescriptor, attributeTagL1ProjectionHandler, redisRuntimeGate);
  }
}
