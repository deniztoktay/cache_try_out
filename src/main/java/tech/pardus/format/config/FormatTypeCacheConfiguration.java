package tech.pardus.format.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.format.cache.FormatTypeCacheValueCodec;
import tech.pardus.format.model.FormatTypeModel;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.ReactiveCacheReader;

@Configuration
public class FormatTypeCacheConfiguration {

  @Bean
  ReactiveCacheReader<FormatTypeModel> formatTypeReactiveCacheReader(
      FormatTypeCacheProperties properties,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      FormatTypeCacheValueCodec codec) {
    var namespace = new CacheNamespace(properties.getNamespace());
    return new ReactiveCacheReader<>(namespace, valueStore, indexStore, codec);
  }
}
