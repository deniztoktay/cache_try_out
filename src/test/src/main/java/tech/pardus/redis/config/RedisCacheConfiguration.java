package tech.pardus.redis.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tech.pardus.redis.api.RedisSetStore;
import tech.pardus.redis.api.RedisStreamBus;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.runtime.RedisRuntimeGate;
import tech.pardus.redis.service.LettuceDistributedLockService;
import tech.pardus.redis.service.ReactiveRedisSetStore;
import tech.pardus.redis.service.ReactiveRedisStreamBus;
import tech.pardus.redis.service.ReactiveRedisValueStore;
import tech.pardus.redis.api.DistributedLockService;
import tech.pardus.cache.l1.L1CacheProperties;
import tech.pardus.attribute.config.AttributeCacheProperties;
import tech.pardus.attribute.config.AttributeCacheStreamProperties;
import tech.pardus.attributetag.config.AttributeTagCacheProperties;
import tech.pardus.attributetag.config.AttributeTagCacheStreamProperties;
import tech.pardus.format.config.FormatTypeCacheProperties;
import tech.pardus.reference.config.ReferenceCacheProperties;
import tech.pardus.tag.config.TagCacheProperties;
import tech.pardus.tag.config.TagCacheStreamProperties;

@Configuration
@EnableConfigurationProperties({
  CacheGroomingProperties.class,
  CacheCoordinationProperties.class,
  TagCacheProperties.class,
  TagCacheStreamProperties.class,
  AttributeCacheProperties.class,
  AttributeCacheStreamProperties.class,
  AttributeTagCacheProperties.class,
  AttributeTagCacheStreamProperties.class,
  FormatTypeCacheProperties.class,
  ReferenceCacheProperties.class,
  L1CacheProperties.class
})
public class RedisCacheConfiguration {

  @Bean
  ReactiveStringRedisTemplate reactiveStringRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {
    return new ReactiveStringRedisTemplate(connectionFactory);
  }

  @Bean
  ReactiveRedisTemplate<String, byte[]> reactiveByteRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {
    var keySerializer = new StringRedisSerializer();
    var valueSerializer =
        RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.byteArray());
    RedisSerializationContext<String, byte[]> context =
        RedisSerializationContext.<String, byte[]>newSerializationContext(keySerializer)
            .value(valueSerializer)
            .hashKey(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
            .hashValue(valueSerializer)
            .build();
    return new ReactiveRedisTemplate<>(connectionFactory, context);
  }

  @Bean
  RedisValueStore redisValueStore(ReactiveRedisTemplate<String, byte[]> reactiveByteRedisTemplate) {
    return new ReactiveRedisValueStore(reactiveByteRedisTemplate);
  }

  @Bean
  RedisSetStore redisSetStore(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
    return new ReactiveRedisSetStore(reactiveStringRedisTemplate);
  }

  @Bean
  RedisStreamBus redisStreamBus(
      ReactiveStringRedisTemplate reactiveStringRedisTemplate, RedisRuntimeGate redisRuntimeGate) {
    return new ReactiveRedisStreamBus(reactiveStringRedisTemplate, redisRuntimeGate);
  }

  @Bean
  DistributedLockService distributedLockService(
      ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
    return new LettuceDistributedLockService(reactiveStringRedisTemplate);
  }

  @Bean(name = "redisOwnerId")
  String redisOwnerId() {
    return java.util.UUID.randomUUID().toString();
  }
}
