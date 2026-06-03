package tech.pardus.cache.l2;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisKeyMaintenance;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.CacheValueCodec;
import tech.pardus.redis.cache.Identifiable;

public class ReactiveL2CacheOperations<M extends Identifiable<?>> implements L2CacheOperations<M> {

  private final CacheNamespace namespace;
  private final Duration ttl;
  private final RedisValueStore valueStore;
  private final CacheIndexStore indexStore;
  private final RedisKeyMaintenance keyMaintenance;
  private final CacheValueCodec<M> codec;
  private final Optional<L2AliasKeyExtractor<M>> aliasExtractor;

  public ReactiveL2CacheOperations(
      CacheNamespace namespace,
      Duration ttl,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      RedisKeyMaintenance keyMaintenance,
      CacheValueCodec<M> codec) {
    this(namespace, ttl, valueStore, indexStore, keyMaintenance, codec, Optional.empty());
  }

  public ReactiveL2CacheOperations(
      CacheNamespace namespace,
      Duration ttl,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      RedisKeyMaintenance keyMaintenance,
      CacheValueCodec<M> codec,
      L2AliasKeyExtractor<M> aliasExtractor) {
    this(namespace, ttl, valueStore, indexStore, keyMaintenance, codec, Optional.of(aliasExtractor));
  }

  private ReactiveL2CacheOperations(
      CacheNamespace namespace,
      Duration ttl,
      RedisValueStore valueStore,
      CacheIndexStore indexStore,
      RedisKeyMaintenance keyMaintenance,
      CacheValueCodec<M> codec,
      Optional<L2AliasKeyExtractor<M>> aliasExtractor) {
    this.namespace = namespace;
    this.ttl = ttl;
    this.valueStore = valueStore;
    this.indexStore = indexStore;
    this.keyMaintenance = keyMaintenance;
    this.codec = codec;
    this.aliasExtractor = aliasExtractor;
  }

  @Override
  public Mono<Void> upsert(M model) {
    if (model == null || model.getId() == null) {
      return Mono.empty();
    }
    var indexKey = CacheKeyLayout.liveIndexKey(namespace);
    var memberId = model.getStringId();
    return valueStore
        .setBytes(CacheKeyLayout.liveValueKey(namespace, memberId), codec.encode(model), ttl)
        .then(indexStore.addMembers(indexKey, List.of(memberId), ttl))
        .then(writeAlias(model))
        .then();
  }

  @Override
  public Mono<Void> remove(String primaryMemberId, String aliasMemberIdOrNull) {
    if (primaryMemberId == null) {
      return Mono.empty();
    }
    var indexKey = CacheKeyLayout.liveIndexKey(namespace);
    var keys = new ArrayList<String>();
    keys.add(CacheKeyLayout.liveValueKey(namespace, primaryMemberId));
    if (aliasMemberIdOrNull != null && !aliasMemberIdOrNull.isBlank()) {
      keys.add(CacheKeyLayout.liveValueKey(namespace, aliasMemberIdOrNull));
    }
    return indexStore.removeMember(indexKey, primaryMemberId, ttl).then(keyMaintenance.deleteKeys(keys)).then();
  }

  public Mono<Void> writeAllAliases(List<M> models) {
    return Flux.fromIterable(models).concatMap(model -> writeAlias(model).then()).then();
  }

  private Mono<Boolean> writeAlias(M model) {
    if (aliasExtractor.isEmpty()) {
      return Mono.just(false);
    }
    var aliasId = aliasExtractor.get().aliasMemberId(model);
    if (aliasId.isEmpty()) {
      return Mono.just(false);
    }
    return valueStore.setBytes(
        CacheKeyLayout.liveValueKey(namespace, aliasId.get()), codec.encode(model), ttl);
  }
}
