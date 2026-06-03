package tech.pardus.cache.write;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.cache.l2.L2CacheOperations;
import tech.pardus.redis.cache.Identifiable;

@Slf4j
public class L2OnlyCacheWriteSync<ID, M extends Identifiable<ID>> implements CacheWriteSync<ID, M> {

  private final L2CacheOperations<M> l2;

  public L2OnlyCacheWriteSync(L2CacheOperations<M> l2) {
    this.l2 = l2;
  }

  @Override
  public Mono<Void> afterInsert(M model) {
    return l2.upsert(model).doOnError(ex -> log.warn("L2 sync after insert failed", ex)).onErrorResume(ex -> Mono.empty());
  }

  @Override
  public Mono<Void> afterUpdate(M model) {
    return l2.upsert(model).doOnError(ex -> log.warn("L2 sync after update failed", ex)).onErrorResume(ex -> Mono.empty());
  }

  @Override
  public Mono<Void> afterDelete(ID id, DeleteContext<M> context) {
    var memberId = context.previousModel() != null ? context.previousModel().getStringId() : String.valueOf(id);
    return l2.remove(memberId, context.aliasMemberIdOrNull())
        .doOnError(ex -> log.warn("L2 sync after delete failed for id={}", id, ex))
        .onErrorResume(ex -> Mono.empty());
  }
}
