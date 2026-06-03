package tech.pardus.attribute.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.attribute.cache.AttributeCacheValueCodec;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.cache.l1.ReactiveL1RedisPersister;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.ResizableL1CacheFactory;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.ReactiveCacheReader;

@Configuration
public class AttributeL1CacheConfiguration {

  @Bean
  ResizableL1Cache<String, AttributeModel> attributeL1Cache(
      ResizableL1CacheFactory factory,
      AttributeCacheProperties properties,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      AttributeCacheValueCodec codec) {
    var namespace = new CacheNamespace(properties.getNamespace());
    var persister =
        new ReactiveL1RedisPersister<>(
            namespace, valueStore, indexStore, codec, properties.getTtl());
    return factory.create("attribute-cache", persister);
  }

  @Bean
  ReactiveCacheReader<AttributeModel> attributeReactiveCacheReader(
      AttributeCacheProperties properties,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      AttributeCacheValueCodec codec) {
    var namespace = new CacheNamespace(properties.getNamespace());
    return new ReactiveCacheReader<>(namespace, valueStore, indexStore, codec);
  }

  @Bean
  TieredReactiveCacheReader<AttributeModel> attributeTieredCacheReader(
      ResizableL1Cache<String, AttributeModel> attributeL1Cache,
      AttributeCacheProperties properties,
      ReactiveCacheReader<AttributeModel> attributeReactiveCacheReader,
      AttributeCacheValueCodec codec) {
    return new TieredReactiveCacheReader<>(attributeL1Cache, attributeReactiveCacheReader, codec);
  }
}
