package tech.pardus.newdesign.attribute.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.newdesign.cachekey.AttributeCacheKey;
import tech.pardus.newdesign.read.L1L2SingleValueReadStrategy;
import tech.pardus.newdesign.read.ReadStrategyFactory;
import tech.pardus.newdesign.redis.codec.JacksonValuePayloadCodec;
import tech.pardus.newdesign.redis.codec.ValuePayloadCodec;
import tech.pardus.newdesign.redis.store.RedisByteStore;
import tech.pardus.newdesign.redis.store.RedisIndexStore;
import tech.pardus.newdesign.write.L1L2SingleValueWriteSync;
import tech.pardus.newdesign.write.SingleValueCacheWriteSync;

@Configuration
public class AttributeCacheConfiguration {

  @Bean
  ValuePayloadCodec<AttributeModel> attributeValuePayloadCodec(ObjectMapper objectMapper) {
    return new JacksonValuePayloadCodec<>(objectMapper, AttributeModel.class);
  }

  @Bean
  L1L2SingleValueReadStrategy<AttributeModel> attributeL1L2ReadStrategy(
      AttributeCacheKey attributeCacheKey,
      RedisByteStore redisByteStore,
      ValuePayloadCodec<AttributeModel> codec) {
    return ReadStrategyFactory.requireL1L2SingleValue(
        ReadStrategyFactory.createSingleValue(
            attributeCacheKey, redisByteStore, codec, AttributeModel::getStringId));
  }

  @Bean
  @Qualifier("newDesignAttributeCacheWriteSync")
  SingleValueCacheWriteSync<Integer, AttributeModel> newDesignAttributeCacheWriteSync(
      AttributeCacheKey attributeCacheKey,
      RedisByteStore redisByteStore,
      RedisIndexStore redisIndexStore,
      ValuePayloadCodec<AttributeModel> codec,
      L1L2SingleValueReadStrategy<AttributeModel> readStrategy) {
    return new L1L2SingleValueWriteSync<>(
        attributeCacheKey,
        redisByteStore,
        redisIndexStore,
        codec,
        readStrategy.l1(),
        AttributeModel::getStringId,
        AttributeModel::name);
  }
}
