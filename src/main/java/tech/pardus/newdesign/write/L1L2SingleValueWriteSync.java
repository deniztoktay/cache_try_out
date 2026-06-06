package tech.pardus.newdesign.write;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.cachekey.CacheKey;
import tech.pardus.newdesign.l1.L1EntityMapCache;
import tech.pardus.newdesign.redis.codec.ValuePayloadCodec;
import tech.pardus.newdesign.redis.keys.CacheRedisKeys;
import tech.pardus.newdesign.redis.store.RedisByteStore;
import tech.pardus.newdesign.redis.store.RedisIndexStore;

@Slf4j
public final class L1L2SingleValueWriteSync<ID, M> implements SingleValueCacheWriteSync<ID, M> {

  private final CacheKey cacheKey;
  private final RedisByteStore store;
  private final RedisIndexStore indexStore;
  private final ValuePayloadCodec<M> codec;
  private final L1EntityMapCache<M> l1;
  private final Function<M, String> idExtractor;
  private final Function<M, String> nameAliasExtractor;

  public L1L2SingleValueWriteSync(
      CacheKey cacheKey,
      RedisByteStore store,
      RedisIndexStore indexStore,
      ValuePayloadCodec<M> codec,
      L1EntityMapCache<M> l1,
      Function<M, String> idExtractor,
      Function<M, String> nameAliasExtractor) {
    this.cacheKey = cacheKey;
    this.store = store;
    this.indexStore = indexStore;
    this.codec = codec;
    this.l1 = l1;
    this.idExtractor = idExtractor;
    this.nameAliasExtractor = nameAliasExtractor;
  }

  @Override
  public Mono<Void> afterInsert(M model) {
    return writeThrough(model).doOnSuccess(v -> indexL1(model));
  }

  @Override
  public Mono<Void> afterUpdate(M model) {
    return writeThrough(model).doOnSuccess(v -> indexL1(model));
  }

  @Override
  public Mono<Void> afterDelete(ID id, DeleteContext<M> context) {
    var model = context.previousModel();
    if (model == null) {
      return Mono.empty();
    }
    var memberId = idExtractor.apply(model);
    return deleteValue(memberId)
        .then(deleteNamed(model))
        .then(indexStore.removeMember(cacheKey, memberId))
        .doOnSuccess(v -> l1.remove(memberId))
        .doOnError(ex -> log.warn("L1_L2 sync after delete failed for id={}", id, ex))
        .onErrorResume(ex -> Mono.empty());
  }

  private Mono<Void> writeThrough(M model) {
    var memberId = idExtractor.apply(model);
    if (memberId == null || memberId.isBlank()) {
      return Mono.empty();
    }
    var payload = codec.encode(model);
    return store
        .set(CacheRedisKeys.valueKey(cacheKey, memberId), payload, cacheKey.ttl())
        .then(writeNamed(model))
        .then(indexStore.addMember(cacheKey, memberId, cacheKey.ttl()))
        .doOnError(ex -> log.warn("L1_L2 sync write failed for id={}", memberId, ex))
        .onErrorResume(ex -> Mono.empty());
  }

  private Mono<Void> writeNamed(M model) {
    var alias = nameAliasExtractor.apply(model);
    if (alias == null || alias.isBlank()) {
      return Mono.empty();
    }
    return store.set(
        CacheRedisKeys.namedKey(cacheKey, alias), codec.encode(model), cacheKey.ttl());
  }

  private Mono<Void> deleteValue(String memberId) {
    return store.delete(CacheRedisKeys.valueKey(cacheKey, memberId));
  }

  private Mono<Void> deleteNamed(M model) {
    var alias = nameAliasExtractor.apply(model);
    if (alias == null || alias.isBlank()) {
      return Mono.empty();
    }
    return store.delete(CacheRedisKeys.namedKey(cacheKey, alias));
  }

  private void indexL1(M model) {
    var memberId = idExtractor.apply(model);
    if (memberId != null && !memberId.isBlank()) {
      l1.put(memberId, model);
    }
  }
}
