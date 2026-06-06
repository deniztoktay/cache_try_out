package tech.pardus.newdesign.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import tech.pardus.newdesign.redis.store.ReactiveRedisByteStore;
import tech.pardus.newdesign.redis.store.ReactiveRedisIndexStore;
import tech.pardus.newdesign.redis.store.ReactiveRedisMetaStore;
import tech.pardus.newdesign.redis.store.RedisByteStore;
import tech.pardus.newdesign.redis.store.RedisIndexStore;
import tech.pardus.newdesign.redis.store.RedisMetaStore;

/** Wires Spring Redis connection beans into the new-design store layer. */
@Configuration
public class NewDesignRedisConfiguration {

  @Bean
  RedisByteStore newDesignRedisByteStore(
      ReactiveRedisTemplate<String, byte[]> reactiveByteRedisTemplate) {
    return new ReactiveRedisByteStore(reactiveByteRedisTemplate);
  }

  @Bean
  RedisIndexStore newDesignRedisIndexStore(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
    return new ReactiveRedisIndexStore(reactiveStringRedisTemplate);
  }

  @Bean
  RedisMetaStore newDesignRedisMetaStore(
      ReactiveRedisTemplate<String, byte[]> reactiveByteRedisTemplate) {
    return new ReactiveRedisMetaStore(reactiveByteRedisTemplate);
  }
}
