package tech.pardus.newdesign.tag.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.newdesign.cachekey.TagCacheKey;
import tech.pardus.newdesign.read.L1L2SingleValueReadStrategy;
import tech.pardus.newdesign.read.ReadStrategyFactory;
import tech.pardus.newdesign.redis.codec.JacksonValuePayloadCodec;
import tech.pardus.newdesign.redis.codec.ValuePayloadCodec;
import tech.pardus.newdesign.redis.store.RedisByteStore;
import tech.pardus.newdesign.redis.store.RedisIndexStore;
import tech.pardus.newdesign.write.L1L2SingleValueWriteSync;
import tech.pardus.newdesign.write.SingleValueCacheWriteSync;
import tech.pardus.tag.model.TagModel;

@Configuration
public class TagCacheConfiguration {

  @Bean
  ValuePayloadCodec<TagModel> tagValuePayloadCodec(ObjectMapper objectMapper) {
    return new JacksonValuePayloadCodec<>(objectMapper, TagModel.class);
  }

  @Bean
  L1L2SingleValueReadStrategy<TagModel> tagL1L2ReadStrategy(
      TagCacheKey tagCacheKey, RedisByteStore redisByteStore, ValuePayloadCodec<TagModel> codec) {
    return ReadStrategyFactory.requireL1L2SingleValue(
        ReadStrategyFactory.createSingleValue(
            tagCacheKey, redisByteStore, codec, TagModel::getStringId));
  }

  @Bean
  @Qualifier("newDesignTagCacheWriteSync")
  SingleValueCacheWriteSync<Integer, TagModel> newDesignTagCacheWriteSync(
      TagCacheKey tagCacheKey,
      RedisByteStore redisByteStore,
      RedisIndexStore redisIndexStore,
      ValuePayloadCodec<TagModel> codec,
      L1L2SingleValueReadStrategy<TagModel> readStrategy) {
    return new L1L2SingleValueWriteSync<>(
        tagCacheKey,
        redisByteStore,
        redisIndexStore,
        codec,
        readStrategy.l1(),
        TagModel::getStringId,
        TagModel::name);
  }
}
