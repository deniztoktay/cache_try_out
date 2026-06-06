package tech.pardus.newdesign.read;

import java.util.function.Function;
import tech.pardus.newdesign.cachekey.ApplicationCache;
import tech.pardus.newdesign.cachekey.CacheKey;
import tech.pardus.newdesign.cachekey.CacheKeyFactory;
import tech.pardus.newdesign.l1.CaffeineL1EntityMapCache;
import tech.pardus.newdesign.l1.L1EntityMapCache;
import tech.pardus.newdesign.redis.codec.ListPayloadCodec;
import tech.pardus.newdesign.redis.codec.ValuePayloadCodec;
import tech.pardus.newdesign.redis.read.RedisGroupedCollectionReader;
import tech.pardus.newdesign.redis.read.RedisSingleValueReader;
import tech.pardus.newdesign.redis.store.RedisByteStore;

/** Creates tier-specific read strategies for single values and grouped collections. */
public final class ReadStrategyFactory {

  private ReadStrategyFactory() {}

  public static <T> SingleValueReadStrategy<T> createSingleValue(
      CacheKeyFactory cacheKeyFactory,
      ApplicationCache cache,
      RedisByteStore redisStore,
      ValuePayloadCodec<T> codec,
      Function<T, String> idExtractor) {
    return createSingleValue(cacheKeyFactory.get(cache), redisStore, codec, idExtractor);
  }

  public static <T> SingleValueReadStrategy<T> createSingleValue(
      CacheKeyFactory cacheKeyFactory,
      ApplicationCache cache,
      RedisByteStore redisStore,
      ValuePayloadCodec<T> codec) {
    return createSingleValue(cacheKeyFactory.get(cache), redisStore, codec, value -> null);
  }

  public static <T> SingleValueReadStrategy<T> createSingleValue(
      CacheKey cacheKey,
      RedisByteStore redisStore,
      ValuePayloadCodec<T> codec,
      Function<T, String> idExtractor) {
    return createSingleValue(cacheKey, cacheKey.readTier(), redisStore, codec, idExtractor);
  }

  public static <T> SingleValueReadStrategy<T> createSingleValue(
      CacheKey cacheKey,
      ReadTier tier,
      RedisByteStore redisStore,
      ValuePayloadCodec<T> codec,
      Function<T, String> idExtractor) {
    return switch (tier) {
      case DB_ONLY -> new DbOnlySingleValueStrategy<>();
      case L2_ONLY ->
          new L2OnlySingleValueStrategy<>(
              new RedisSingleValueReader<>(cacheKey, redisStore, codec));
      case L1_L2 -> {
        if (!cacheKey.supportsL1()) {
          throw new IllegalStateException(
              "L1_L2 requires positive maxCapacity on CacheKey: " + cacheKey.getKey());
        }
        L1EntityMapCache<T> l1 = new CaffeineL1EntityMapCache<>(cacheKey);
        RedisSingleValueReader<T> redis =
            new RedisSingleValueReader<>(cacheKey, redisStore, codec);
        yield new L1L2SingleValueReadStrategy<>(l1, redis, idExtractor);
      }
    };
  }

  public static <T> GroupedCollectionReadStrategy<T> createGroupedCollection(
      CacheKeyFactory cacheKeyFactory,
      ApplicationCache cache,
      RedisByteStore redisStore,
      ListPayloadCodec<T> codec,
      Function<T, String> idExtractor) {
    return createGroupedCollection(cacheKeyFactory.get(cache), redisStore, codec, idExtractor);
  }

  public static <T> GroupedCollectionReadStrategy<T> createGroupedCollection(
      CacheKey cacheKey,
      RedisByteStore redisStore,
      ListPayloadCodec<T> codec,
      Function<T, String> idExtractor) {
    return createGroupedCollection(cacheKey, cacheKey.readTier(), redisStore, codec, idExtractor);
  }

  public static <T> GroupedCollectionReadStrategy<T> createGroupedCollection(
      CacheKey cacheKey,
      ReadTier tier,
      RedisByteStore redisStore,
      ListPayloadCodec<T> codec,
      Function<T, String> idExtractor) {
    return switch (tier) {
      case DB_ONLY -> new DbOnlyGroupedCollectionStrategy<>();
      case L2_ONLY ->
          new L2OnlyGroupedCollectionStrategy<>(
              new RedisGroupedCollectionReader<>(cacheKey, redisStore, codec));
      case L1_L2 -> {
        if (!cacheKey.supportsL1()) {
          throw new IllegalStateException(
              "L1_L2 requires positive maxCapacity on CacheKey: " + cacheKey.getKey());
        }
        L1EntityMapCache<T> l1 = new CaffeineL1EntityMapCache<>(cacheKey);
        RedisGroupedCollectionReader<T> redis =
            new RedisGroupedCollectionReader<>(cacheKey, redisStore, codec);
        yield new L1L2GroupedCollectionStrategy<>(l1, redis, idExtractor);
      }
    };
  }

  public static <T> L1L2SingleValueReadStrategy<T> requireL1L2SingleValue(
      SingleValueReadStrategy<T> strategy) {
    if (strategy instanceof L1L2SingleValueReadStrategy<T> l1L2) {
      return l1L2;
    }
    throw new IllegalStateException("Expected L1_L2 strategy but was " + strategy.tier());
  }

  public static <T> L1L2GroupedCollectionStrategy<T> requireL1L2GroupedCollection(
      GroupedCollectionReadStrategy<T> strategy) {
    if (strategy instanceof L1L2GroupedCollectionStrategy<T> l1L2) {
      return l1L2;
    }
    throw new IllegalStateException("Expected L1_L2 strategy but was " + strategy.tier());
  }
}
