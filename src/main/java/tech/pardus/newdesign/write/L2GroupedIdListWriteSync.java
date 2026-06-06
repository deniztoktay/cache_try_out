package tech.pardus.newdesign.write;

import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.cachekey.CacheKey;
import tech.pardus.newdesign.redis.codec.ListPayloadCodec;
import tech.pardus.newdesign.redis.keys.CacheRedisKeys;
import tech.pardus.newdesign.redis.store.RedisByteStore;
import tech.pardus.newdesign.redis.store.RedisIndexStore;

@Slf4j
public final class L2GroupedIdListWriteSync implements GroupedIdListCacheWriteSync {

  private final CacheKey cacheKey;
  private final RedisByteStore store;
  private final RedisIndexStore indexStore;
  private final ListPayloadCodec<Integer> codec;
  private final Function<Integer, Mono<java.util.List<Integer>>> valueGroupLoader;
  private final Function<Integer, Mono<java.util.List<Integer>>> namedGroupLoader;
  private final BiFunction<Integer, Integer, String> memberIdFn;

  public L2GroupedIdListWriteSync(
      CacheKey cacheKey,
      RedisByteStore store,
      RedisIndexStore indexStore,
      ListPayloadCodec<Integer> codec,
      Function<Integer, Mono<java.util.List<Integer>>> valueGroupLoader,
      Function<Integer, Mono<java.util.List<Integer>>> namedGroupLoader,
      BiFunction<Integer, Integer, String> memberIdFn) {
    this.cacheKey = cacheKey;
    this.store = store;
    this.indexStore = indexStore;
    this.codec = codec;
    this.valueGroupLoader = valueGroupLoader;
    this.namedGroupLoader = namedGroupLoader;
    this.memberIdFn = memberIdFn;
  }

  @Override
  public Mono<Void> afterLinkInsert(Integer valueGroupId, Integer namedGroupId) {
    return refreshGroups(valueGroupId, namedGroupId)
        .then(indexStore.addMember(cacheKey, memberIdFn.apply(valueGroupId, namedGroupId), cacheKey.ttl()))
        .doOnError(ex -> log.warn("L2 id-list sync after insert failed", ex))
        .onErrorResume(ex -> Mono.empty());
  }

  @Override
  public Mono<Void> afterLinkDelete(Integer valueGroupId, Integer namedGroupId) {
    return refreshGroups(valueGroupId, namedGroupId)
        .then(indexStore.removeMember(cacheKey, memberIdFn.apply(valueGroupId, namedGroupId)))
        .doOnError(ex -> log.warn("L2 id-list sync after delete failed", ex))
        .onErrorResume(ex -> Mono.empty());
  }

  private Mono<Void> refreshGroups(Integer valueGroupId, Integer namedGroupId) {
    return refreshValueGroup(valueGroupId).then(refreshNamedGroup(namedGroupId));
  }

  private Mono<Void> refreshValueGroup(Integer valueGroupId) {
    if (valueGroupId == null) {
      return Mono.empty();
    }
    return valueGroupLoader
        .apply(valueGroupId)
        .flatMap(
            ids ->
                store.set(
                    CacheRedisKeys.valueKey(cacheKey, String.valueOf(valueGroupId)),
                    codec.encode(ids),
                    cacheKey.ttl()))
        .then();
  }

  private Mono<Void> refreshNamedGroup(Integer namedGroupId) {
    if (namedGroupId == null) {
      return Mono.empty();
    }
    return namedGroupLoader
        .apply(namedGroupId)
        .flatMap(
            ids ->
                store.set(
                    CacheRedisKeys.namedKey(cacheKey, String.valueOf(namedGroupId)),
                    codec.encode(ids),
                    cacheKey.ttl()))
        .then();
  }
}
