package tech.pardus.tag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.cache.l1.ReactiveL1RedisPersister;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.ResizableL1CacheFactory;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.ReactiveCacheReader;
import tech.pardus.tag.cache.TagCacheValueCodec;
import tech.pardus.tag.model.TagModel;

@Configuration
public class TagL1CacheConfiguration {

  @Bean
  ResizableL1Cache<String, TagModel> tagL1Cache(
      ResizableL1CacheFactory factory,
      TagCacheProperties tagProperties,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      TagCacheValueCodec codec) {
    var namespace = new CacheNamespace(tagProperties.getNamespace());
    var persister =
        new ReactiveL1RedisPersister<>(
            namespace, valueStore, indexStore, codec, tagProperties.getTtl());
    return factory.create("tag-cache", persister);
  }

  @Bean
  ReactiveCacheReader<TagModel> tagReactiveCacheReader(
      TagCacheProperties tagProperties,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      TagCacheValueCodec codec) {
    var namespace = new CacheNamespace(tagProperties.getNamespace());
    return new ReactiveCacheReader<>(namespace, valueStore, indexStore, codec);
  }

  @Bean
  TieredReactiveCacheReader<TagModel> tagTieredCacheReader(
      ResizableL1Cache<String, TagModel> tagL1Cache,
      ReactiveCacheReader<TagModel> tagReactiveCacheReader,
      TagCacheValueCodec codec) {
    return new TieredReactiveCacheReader<>(tagL1Cache, tagReactiveCacheReader, codec);
  }
}
