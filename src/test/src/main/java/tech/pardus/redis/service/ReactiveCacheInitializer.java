package tech.pardus.redis.service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.CacheInitializer;
import tech.pardus.redis.api.RedisKeyMaintenance;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.CacheValueCodec;
import tech.pardus.redis.cache.Identifiable;
import tech.pardus.redis.support.TtlPolicy;

/**
 * Loads data into a temporary index + value keys, then promotes them to the live namespace: value
 * keys are renamed first so data remains addressable, then the live index is replaced.
 */
@Slf4j
@Component
public class ReactiveCacheInitializer implements CacheInitializer {
  private final RedisValueStore values;
  private final CacheIndexStore index;
  private final RedisKeyMaintenance maintenance;

  public ReactiveCacheInitializer(
      RedisValueStore values, CacheIndexStore index, RedisKeyMaintenance maintenance) {
    this.values = values;
    this.index = index;
    this.maintenance = maintenance;
  }

  @Override
  public <T extends Identifiable<?>> Mono<Void> initialize(
      CacheNamespace namespace, Duration ttl, Mono<List<T>> dbFetchTask, CacheValueCodec<T> codec) {
    TtlPolicy.requirePositive(ttl, "initialize");
    var runId = UUID.randomUUID().toString();
    var tempIndexKey = CacheKeyLayout.tempIndexKey(namespace, runId);

    return dbFetchTask
        .flatMap(
            items ->
                persistTemporary(namespace, runId, tempIndexKey, ttl, items, codec)
                    .then(promoteTemporaryToLive(namespace, runId, tempIndexKey, ttl)))
        .doOnSuccess(v -> log.info("Cache initialized for namespace {}", namespace.name()));
  }

  @Override
  public <T extends Identifiable<?>> Mono<Void> initialize(
      CacheNamespace namespace, Duration ttl, List<T> prefetched, CacheValueCodec<T> codec) {
    return initialize(namespace, ttl, Mono.just(prefetched), codec);
  }

  private <T extends Identifiable<?>> Mono<Void> persistTemporary(
      CacheNamespace namespace,
      String runId,
      String tempIndexKey,
      Duration ttl,
      List<T> items,
      CacheValueCodec<T> codec) {

    if (items.isEmpty()) {
      return clearLiveIndex(namespace, ttl);
    }

    return Flux.fromIterable(items)
        .concatMap(
            item -> {
              var stringId = codec.stringId(item);
              var valueKey = CacheKeyLayout.tempValueKey(namespace, runId, stringId);
              return values
                  .setBytes(valueKey, codec.encode(item), ttl)
                  .then(index.addMembers(tempIndexKey, List.of(stringId), ttl));
            })
        .then();
  }

  private Mono<Void> promoteTemporaryToLive(
      CacheNamespace namespace, String runId, String tempIndexKey, Duration ttl) {

    var liveIndexKey = CacheKeyLayout.liveIndexKey(namespace);

    return index
        .listMembers(tempIndexKey)
        .collectList()
        .flatMap(
            newMemberIds -> {
              if (newMemberIds.isEmpty()) {
                return maintenance.deleteKey(tempIndexKey).then(clearLiveIndex(namespace, ttl));
              }
              return index
                  .listMembers(liveIndexKey)
                  .collectList()
                  .defaultIfEmpty(List.of())
                  .flatMap(
                      oldMemberIds -> {
                        var newIdSet = new HashSet<>(newMemberIds);
                        return renameTempValuesToLive(namespace, runId, newMemberIds)
                            .then(replaceLiveIndex(liveIndexKey, tempIndexKey, ttl))
                            .then(deleteOrphanedValues(namespace, oldMemberIds, newIdSet))
                            .then(cleanupTempIndexIfNeeded(tempIndexKey, liveIndexKey, ttl));
                      });
            })
        .then();
  }

  private Mono<Void> clearLiveIndex(CacheNamespace namespace, Duration ttl) {
    var liveIndexKey = CacheKeyLayout.liveIndexKey(namespace);
    return index
        .listMembers(liveIndexKey)
        .collectList()
        .defaultIfEmpty(List.of())
        .flatMap(
            oldMemberIds ->
                deleteOrphanedValues(namespace, oldMemberIds, Set.of())
                    .then(maintenance.deleteKey(liveIndexKey)))
        .then();
  }

  private Mono<Void> renameTempValuesToLive(
      CacheNamespace namespace, String runId, List<String> memberIds) {
    return Flux.fromIterable(memberIds)
        .concatMap(
            memberId ->
                maintenance.renameKey(
                    CacheKeyLayout.tempValueKey(namespace, runId, memberId),
                    CacheKeyLayout.liveValueKey(namespace, memberId)))
        .then();
  }

  private Mono<Void> replaceLiveIndex(String liveIndexKey, String tempIndexKey, Duration ttl) {
    return maintenance
        .deleteKey(liveIndexKey)
        .then(maintenance.renameKey(tempIndexKey, liveIndexKey))
        .flatMap(renamed -> Boolean.TRUE.equals(renamed) ? index.touchTtl(liveIndexKey, ttl) : Mono.empty())
        .then();
  }

  private Mono<Void> deleteOrphanedValues(
      CacheNamespace namespace, List<String> oldMemberIds, Set<String> newIdSet) {
    return Flux.fromIterable(oldMemberIds)
        .filter(oldId -> !newIdSet.contains(oldId))
        .flatMap(
            staleId -> maintenance.deleteKey(CacheKeyLayout.liveValueKey(namespace, staleId)))
        .then();
  }

  private Mono<Void> cleanupTempIndexIfNeeded(
      String tempIndexKey, String liveIndexKey, Duration ttl) {
    if (tempIndexKey.equals(liveIndexKey)) {
      return Mono.empty();
    }
    return maintenance.deleteKey(tempIndexKey).then(index.touchTtl(liveIndexKey, ttl)).then();
  }
}
