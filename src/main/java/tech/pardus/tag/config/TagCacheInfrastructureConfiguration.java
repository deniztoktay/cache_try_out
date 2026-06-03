package tech.pardus.tag.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import tech.pardus.jdbc.tag.view.TagL1ProjectionHandler;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisKeyMaintenance;
import tech.pardus.redis.api.RedisStreamBus;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.ReactiveCacheReader;
import tech.pardus.redis.coordination.CacheL2ReadyMarker;
import tech.pardus.redis.runtime.RedisRuntimeGate;
import tech.pardus.tag.cache.TagCacheValueCodec;
import tech.pardus.tag.cache.TagEntityLoader;
import tech.pardus.tag.model.TagModel;

@Configuration
public class TagCacheInfrastructureConfiguration {

  @Bean
  CacheEntityDescriptor tagCacheDescriptor(
      TagCacheProperties cacheProperties, TagCacheStreamProperties streamProperties) {
    var stream = new StreamConfig();
    stream.setListenerEnabled(streamProperties.isListenerEnabled());
    stream.setRetentionTtl(streamProperties.getRetentionTtl());
    stream.setPollBlock(streamProperties.getPollBlock());
    stream.setBatchSize(streamProperties.getBatchSize());
    return CacheEntityDescriptor.l1L2(
        "tag",
        new CacheNamespace(cacheProperties.getNamespace()),
        cacheProperties.getTtl(),
        stream);
  }

  @Bean
  L2CacheOperations<TagModel> tagL2CacheOperations(
      CacheEntityDescriptor tagCacheDescriptor,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      RedisKeyMaintenance keyMaintenance,
      TagCacheValueCodec codec) {
    L2AliasKeyExtractor<TagModel> alias =
        model ->
            model.name() != null && !model.name().isBlank()
                ? java.util.Optional.of(model.nameAliasStringId())
                : java.util.Optional.empty();
    return new ReactiveL2CacheOperations<>(
        tagCacheDescriptor.namespace(),
        tagCacheDescriptor.ttl(),
        valueStore,
        indexStore,
        keyMaintenance,
        codec,
        alias);
  }

  @Bean
  CacheReadStrategy<Integer, TagModel> tagCacheReadStrategy(
      @Qualifier("tagCacheDescriptor") CacheEntityDescriptor tagCacheDescriptor,
      TagEntityLoader tagEntityLoader,
      @Qualifier("tagReactiveCacheReader") ReactiveCacheReader<TagModel> tagReactiveCacheReader,
      @Qualifier("tagTieredCacheReader") TieredReactiveCacheReader<TagModel> tagTieredCacheReader,
      @Qualifier("tagL1Cache") ResizableL1Cache<String, TagModel> tagL1Cache,
      CacheL2ReadyMarker l2ReadyMarker,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      TagCacheValueCodec codec) {

    var dependencies =
        CacheReadStrategyDependencies.<Integer, TagModel>builder()
            .l2Reader(tagReactiveCacheReader)
            .tieredReader(tagTieredCacheReader)
            .l1Cache(tagL1Cache)
            .l2ReadyMarker(l2ReadyMarker)
            .valueStore(valueStore)
            .indexStore(indexStore)
            .codec(codec)
            .build();

    return CacheReadStrategyFactory.create(tagCacheDescriptor, tagEntityLoader, dependencies);
  }

  @Bean
  CacheWriteSync<Integer, TagModel> tagCacheWriteSync(
      CacheEntityDescriptor tagCacheDescriptor,
      L2CacheOperations<TagModel> tagL2CacheOperations,
      CacheEventPublisher tagCacheEventPublisher) {
    return CacheWriteSyncFactory.create(
        tagCacheDescriptor, tagL2CacheOperations, tagCacheEventPublisher);
  }

  @Bean
  CacheEventPublisher tagCacheEventPublisher(
      RedisStreamBus streamBus, CacheEntityDescriptor tagCacheDescriptor) {
    return new CacheEventPublisher(streamBus, tagCacheDescriptor);
  }

  @Bean
  TagL1ProjectionHandler tagL1ProjectionHandler(
      CacheReadStrategy<Integer, TagModel> tagCacheReadStrategy,
      ResizableL1Cache<String, TagModel> tagL1Cache) {
    return new TagL1ProjectionHandler(tagCacheReadStrategy, tagL1Cache);
  }

  @Bean
  CacheStreamListener tagCacheStreamListener(
      RedisStreamBus streamBus,
      CacheEntityDescriptor tagCacheDescriptor,
      TagL1ProjectionHandler tagL1ProjectionHandler,
      RedisRuntimeGate redisRuntimeGate) {
    return new CacheStreamListener(
        streamBus, tagCacheDescriptor, tagL1ProjectionHandler, redisRuntimeGate);
  }
}
