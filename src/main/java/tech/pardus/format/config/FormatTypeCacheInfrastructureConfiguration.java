package tech.pardus.format.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.cache.l2.L2AliasKeyExtractor;
import tech.pardus.cache.l2.L2CacheOperations;
import tech.pardus.cache.l2.ReactiveL2CacheOperations;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.cache.read.CacheReadStrategyDependencies;
import tech.pardus.cache.read.CacheReadStrategyFactory;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.cache.write.CacheWriteSync;
import tech.pardus.cache.write.CacheWriteSyncFactory;
import tech.pardus.format.cache.FormatTypeCacheValueCodec;
import tech.pardus.format.cache.FormatTypeEntityLoader;
import tech.pardus.format.model.FormatTypeModel;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisKeyMaintenance;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.ReactiveCacheReader;

@Configuration
public class FormatTypeCacheInfrastructureConfiguration {

  @Bean
  CacheEntityDescriptor formatTypeCacheDescriptor(FormatTypeCacheProperties cacheProperties) {
    return CacheEntityDescriptor.l2Only(
        "format-type",
        new CacheNamespace(cacheProperties.getNamespace()),
        cacheProperties.getTtl());
  }

  @Bean
  L2CacheOperations<FormatTypeModel> formatTypeL2CacheOperations(
      CacheEntityDescriptor formatTypeCacheDescriptor,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      RedisKeyMaintenance keyMaintenance,
      FormatTypeCacheValueCodec codec) {
    L2AliasKeyExtractor<FormatTypeModel> alias =
        model ->
            model.formatValue() != null
                ? java.util.Optional.of(model.valueCultureAliasStringId())
                : java.util.Optional.empty();
    return new ReactiveL2CacheOperations<>(
        formatTypeCacheDescriptor.namespace(),
        formatTypeCacheDescriptor.ttl(),
        valueStore,
        indexStore,
        keyMaintenance,
        codec,
        alias);
  }

  @Bean
  CacheReadStrategy<Integer, FormatTypeModel> formatTypeCacheReadStrategy(
      CacheEntityDescriptor formatTypeCacheDescriptor,
      FormatTypeEntityLoader formatTypeEntityLoader,
      ReactiveCacheReader<FormatTypeModel> formatTypeReactiveCacheReader) {

    var dependencies =
        CacheReadStrategyDependencies.<Integer, FormatTypeModel>builder()
            .l2Reader(formatTypeReactiveCacheReader)
            .build();

    return CacheReadStrategyFactory.create(
        formatTypeCacheDescriptor, formatTypeEntityLoader, dependencies);
  }

  @Bean
  CacheWriteSync<Integer, FormatTypeModel> formatTypeCacheWriteSync(
      CacheEntityDescriptor formatTypeCacheDescriptor,
      L2CacheOperations<FormatTypeModel> formatTypeL2CacheOperations) {
    return CacheWriteSyncFactory.create(formatTypeCacheDescriptor, formatTypeL2CacheOperations, null);
  }
}
