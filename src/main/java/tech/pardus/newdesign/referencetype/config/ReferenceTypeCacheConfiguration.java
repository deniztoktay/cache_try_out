package tech.pardus.newdesign.referencetype.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.newdesign.cachekey.ReferenceTypeCacheKey;
import tech.pardus.newdesign.read.L1L2SingleValueReadStrategy;
import tech.pardus.newdesign.read.ReadStrategyFactory;
import tech.pardus.newdesign.redis.codec.JacksonValuePayloadCodec;
import tech.pardus.newdesign.redis.codec.ValuePayloadCodec;
import tech.pardus.newdesign.redis.store.RedisByteStore;
import tech.pardus.newdesign.redis.store.RedisIndexStore;
import tech.pardus.newdesign.write.L1L2SingleValueWriteSync;
import tech.pardus.newdesign.write.SingleValueCacheWriteSync;
import tech.pardus.referencetype.model.ReferenceTypeModel;

@Configuration
public class ReferenceTypeCacheConfiguration {

  @Bean
  ValuePayloadCodec<ReferenceTypeModel> referenceTypeValuePayloadCodec(ObjectMapper objectMapper) {
    return new JacksonValuePayloadCodec<>(objectMapper, ReferenceTypeModel.class);
  }

  @Bean
  L1L2SingleValueReadStrategy<ReferenceTypeModel> referenceTypeL1L2ReadStrategy(
      ReferenceTypeCacheKey referenceTypeCacheKey,
      RedisByteStore redisByteStore,
      ValuePayloadCodec<ReferenceTypeModel> codec) {
    return ReadStrategyFactory.requireL1L2SingleValue(
        ReadStrategyFactory.createSingleValue(
            referenceTypeCacheKey, redisByteStore, codec, ReferenceTypeModel::getStringId));
  }

  @Bean
  @Qualifier("newDesignReferenceTypeCacheWriteSync")
  SingleValueCacheWriteSync<Integer, ReferenceTypeModel> newDesignReferenceTypeCacheWriteSync(
      ReferenceTypeCacheKey referenceTypeCacheKey,
      RedisByteStore redisByteStore,
      RedisIndexStore redisIndexStore,
      ValuePayloadCodec<ReferenceTypeModel> codec,
      L1L2SingleValueReadStrategy<ReferenceTypeModel> readStrategy) {
    return new L1L2SingleValueWriteSync<>(
        referenceTypeCacheKey,
        redisByteStore,
        redisIndexStore,
        codec,
        readStrategy.l1(),
        ReferenceTypeModel::getStringId,
        ReferenceTypeModel::name);
  }
}
