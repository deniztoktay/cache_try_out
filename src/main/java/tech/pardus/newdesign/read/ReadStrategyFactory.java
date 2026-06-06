package tech.pardus.newdesign.read;

import java.util.function.Function;
import tech.pardus.newdesign.cachekey.ApplicationCache;
import tech.pardus.newdesign.cachekey.CacheKey;
import tech.pardus.newdesign.cachekey.CacheKeyFactory;
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
      Function<T, String> idExtractor,
      L1EntityMapCache<T> l1) {
    return createSingleValue(
        cacheKeyFactory.get(cache), redisStore, codec, idExtractor, l1);
  }

  public static <T> SingleValueReadStrategy<T> createSingleValue(
      CacheKey cacheKey,
      RedisByteStore redisStore,
      ValuePayloadCodec<T> codec,
      Function<T, String> idExtractor,
      L1EntityMapCache<T> l1) {
    return createSingleValue(cacheKey, cacheKey.readTier(), redisStore, codec, idExtractor, l1);
  }

  public static <T> SingleValueReadStrategy<T> createSingleValue(
      CacheKey cacheKey,
      ReadTier tier,
      RedisByteStore redisStore,
      ValuePayloadCodec<T> codec,
      Function<T, String> idExtractor,
      L1EntityMapCache<T> l1) {
    return switch (tier) {
      case DB_ONLY -> new DbOnlySingleValueStrategy<>();
      case L2_ONLY ->
          new L2OnlySingleValueStrategy<>(
              new RedisSingleValueReader<>(cacheKey, redisStore, codec));
      case L1_L2 -> createL1L2SingleValue(cacheKey, redisStore, codec, l1, idExtractor);
    };
  }

  public static <T> L1L2SingleValueReadStrategy<T> createL1L2SingleValue(
      CacheKey cacheKey,
      RedisByteStore redisStore,
      ValuePayloadCodec<T> codec,
      L1EntityMapCache<T> l1,
      Function<T, String> idExtractor) {
    requireL1(cacheKey, l1);
    return new L1L2SingleValueReadStrategy<>(
        l1, new RedisSingleValueReader<>(cacheKey, redisStore, codec), idExtractor);
  }

  public static <T> GroupedCollectionReadStrategy<T> createGroupedCollection(
      CacheKeyFactory cacheKeyFactory,
      ApplicationCache cache,
      RedisByteStore redisStore,
      ListPayloadCodec<T> codec,
      Function<T, String> idExtractor,
      L1EntityMapCache<T> l1) {
    return createGroupedCollection(
        cacheKeyFactory.get(cache), redisStore, codec, idExtractor, l1);
  }

  public static <T> GroupedCollectionReadStrategy<T> createGroupedCollection(
      CacheKey cacheKey,
      RedisByteStore redisStore,
      ListPayloadCodec<T> codec,
      Function<T, String> idExtractor,
      L1EntityMapCache<T> l1) {
    return createGroupedCollection(cacheKey, cacheKey.readTier(), redisStore, codec, idExtractor, l1);
  }

  public static <T> GroupedCollectionReadStrategy<T> createGroupedCollection(
      CacheKey cacheKey,
      ReadTier tier,
      RedisByteStore redisStore,
      ListPayloadCodec<T> codec,
      Function<T, String> idExtractor,
      L1EntityMapCache<T> l1) {
    return switch (tier) {
      case DB_ONLY -> new DbOnlyGroupedCollectionStrategy<>();
      case L2_ONLY ->
          new L2OnlyGroupedCollectionStrategy<>(
              new RedisGroupedCollectionReader<>(cacheKey, redisStore, codec));
      case L1_L2 -> createL1L2GroupedCollection(cacheKey, redisStore, codec, l1, idExtractor);
    };
  }

  public static <T> L1L2GroupedCollectionStrategy<T> createL1L2GroupedCollection(
      CacheKey cacheKey,
      RedisByteStore redisStore,
      ListPayloadCodec<T> codec,
      L1EntityMapCache<T> l1,
      Function<T, String> idExtractor) {
    requireL1(cacheKey, l1);
    return new L1L2GroupedCollectionStrategy<>(
        l1, new RedisGroupedCollectionReader<>(cacheKey, redisStore, codec), idExtractor);
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

  private static void requireL1(CacheKey cacheKey, L1EntityMapCache<?> l1) {
    if (!cacheKey.supportsL1()) {
      throw new IllegalStateException(
          "L1_L2 requires positive maxCapacity on CacheKey: " + cacheKey.getKey());
    }
    if (l1 == null) {
      throw new IllegalStateException(
          "L1EntityMapCache bean is required for L1_L2 cache: " + cacheKey.getKey());
    }
  }
}
