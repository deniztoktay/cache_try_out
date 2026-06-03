package tech.pardus.reference.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.cache.read.CacheReadStrategyDependencies;
import tech.pardus.cache.read.CacheReadStrategyFactory;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.reference.cache.ReferenceEntityLoader;
import tech.pardus.reference.model.ReferenceModel;
import tech.pardus.redis.cache.CacheNamespace;

@Configuration
public class ReferenceCacheInfrastructureConfiguration {

  @Bean
  CacheEntityDescriptor referenceCacheDescriptor(ReferenceCacheProperties cacheProperties) {
    return CacheEntityDescriptor.dbOnly(
        "reference",
        new CacheNamespace(cacheProperties.getNamespace()),
        cacheProperties.getTtl());
  }

  @Bean
  CacheReadStrategy<Integer, ReferenceModel> referenceCacheReadStrategy(
      CacheEntityDescriptor referenceCacheDescriptor,
      ReferenceEntityLoader referenceEntityLoader) {
    return CacheReadStrategyFactory.create(
        referenceCacheDescriptor,
        referenceEntityLoader,
        CacheReadStrategyDependencies.<Integer, ReferenceModel>builder().build());
  }
}
