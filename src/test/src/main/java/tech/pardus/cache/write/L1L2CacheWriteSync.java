package tech.pardus.cache.write;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.cache.l2.L2CacheOperations;
import tech.pardus.cache.projection.CacheEventPublisher;
import tech.pardus.redis.cache.Identifiable;
import tech.pardus.redis.dto.CacheChangeOperation;

@Slf4j
public class L1L2CacheWriteSync<ID, M extends Identifiable<ID>> implements CacheWriteSync<ID, M> {

  private final L2CacheOperations<M> l2;
  private final CacheEventPublisher eventPublisher;

  public L1L2CacheWriteSync(L2CacheOperations<M> l2, CacheEventPublisher eventPublisher) {
    this.l2 = l2;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Mono<Void> afterInsert(M model) {
    return l2.upsert(model)
        .then(eventPublisher.publish(CacheChangeOperation.INSERT, List.of(model.getStringId())))
        .then()
        .doOnError(ex -> log.warn("L1_L2 sync after insert failed for id={}", model.getId(), ex))
        .onErrorResume(ex -> Mono.empty());
  }

  @Override
  public Mono<Void> afterUpdate(M model) {
    return l2.upsert(model)
        .then(eventPublisher.publish(CacheChangeOperation.UPDATE, List.of(model.getStringId())))
        .then()
        .doOnError(ex -> log.warn("L1_L2 sync after update failed for id={}", model.getId(), ex))
        .onErrorResume(ex -> Mono.empty());
  }

  @Override
  public Mono<Void> afterDelete(ID id, DeleteContext<M> context) {
    var memberId = context.previousModel() != null ? context.previousModel().getStringId() : String.valueOf(id);
    return l2.remove(memberId, context.aliasMemberIdOrNull())
        .then(eventPublisher.publish(CacheChangeOperation.DELETE, List.of(memberId)))
        .then()
        .doOnError(ex -> log.warn("L1_L2 sync after delete failed for id={}", id, ex))
        .onErrorResume(ex -> Mono.empty());
  }
}
