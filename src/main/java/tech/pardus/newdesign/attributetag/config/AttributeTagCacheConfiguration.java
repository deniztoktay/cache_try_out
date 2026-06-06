package tech.pardus.newdesign.attributetag.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.pardus.newdesign.attributetag.loader.AttributeTagEntityLoader;
import tech.pardus.newdesign.cachekey.AttributeTagCacheKey;
import tech.pardus.newdesign.cachekey.CacheEntityId;
import tech.pardus.newdesign.read.GroupedCollectionReadStrategy;
import tech.pardus.newdesign.read.ReadStrategyFactory;
import tech.pardus.newdesign.redis.codec.JacksonListPayloadCodec;
import tech.pardus.newdesign.redis.codec.ListPayloadCodec;
import tech.pardus.newdesign.redis.store.RedisByteStore;
import tech.pardus.newdesign.redis.store.RedisIndexStore;
import tech.pardus.newdesign.write.GroupedIdListCacheWriteSync;
import tech.pardus.newdesign.write.L2GroupedIdListWriteSync;

@Configuration
public class AttributeTagCacheConfiguration {

  @Bean
  ListPayloadCodec<Integer> attributeTagIdListCodec(ObjectMapper objectMapper) {
    return new JacksonListPayloadCodec<>(objectMapper, Integer.class);
  }

  @Bean
  GroupedCollectionReadStrategy<Integer> attributeTagIdListReadStrategy(
      AttributeTagCacheKey attributeTagCacheKey,
      RedisByteStore redisByteStore,
      ListPayloadCodec<Integer> attributeTagIdListCodec) {
    return ReadStrategyFactory.createGroupedCollection(
        attributeTagCacheKey,
        redisByteStore,
        attributeTagIdListCodec,
        String::valueOf);
  }

  @Bean
  @Qualifier("newDesignAttributeTagCacheWriteSync")
  GroupedIdListCacheWriteSync newDesignAttributeTagCacheWriteSync(
      AttributeTagCacheKey attributeTagCacheKey,
      RedisByteStore redisByteStore,
      RedisIndexStore redisIndexStore,
      ListPayloadCodec<Integer> attributeTagIdListCodec,
      AttributeTagEntityLoader loader) {
    return new L2GroupedIdListWriteSync(
        attributeTagCacheKey,
        redisByteStore,
        redisIndexStore,
        attributeTagIdListCodec,
        loader::findTagIdsByAttributeId,
        loader::findAttributeIdsByTagId,
        CacheEntityId::attributeTagMember);
  }
}
