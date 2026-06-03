package tech.pardus.attributetag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.attributetag.cache.AttributeTagCacheValueCodec;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.cache.l1.ReactiveL1RedisPersister;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.ResizableL1CacheFactory;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.ReactiveCacheReader;

@Configuration
public class AttributeTagL1CacheConfiguration {

  @Bean
  ResizableL1Cache<String, AttributeTagModel> attributeTagL1Cache(
      ResizableL1CacheFactory factory,
      AttributeTagCacheProperties properties,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      AttributeTagCacheValueCodec codec) {
    var namespace = new CacheNamespace(properties.getNamespace());
    var persister =
        new ReactiveL1RedisPersister<>(
            namespace, valueStore, indexStore, codec, properties.getTtl());
    return factory.create("attribute-tag-cache", persister);
  }

  @Bean
  ReactiveCacheReader<AttributeTagModel> attributeTagReactiveCacheReader(
      AttributeTagCacheProperties properties,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      AttributeTagCacheValueCodec codec) {
    var namespace = new CacheNamespace(properties.getNamespace());
    return new ReactiveCacheReader<>(namespace, valueStore, indexStore, codec);
  }

  @Bean
  TieredReactiveCacheReader<AttributeTagModel> attributeTagTieredCacheReader(
      ResizableL1Cache<String, AttributeTagModel> attributeTagL1Cache,
      AttributeTagCacheProperties properties,
      ReactiveCacheReader<AttributeTagModel> attributeTagReactiveCacheReader,
      AttributeTagCacheValueCodec codec) {
    return new TieredReactiveCacheReader<>(
        attributeTagL1Cache, attributeTagReactiveCacheReader, codec);
  }
}
