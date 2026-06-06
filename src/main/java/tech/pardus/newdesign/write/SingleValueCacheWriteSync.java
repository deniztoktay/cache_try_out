package tech.pardus.newdesign.write;

import reactor.core.publisher.Mono;

/** Post-commit L2/L1 synchronization for single-value caches ({@code v:{id}} / {@code v:n:{alias}}). */
public interface SingleValueCacheWriteSync<ID, M> {

  Mono<Void> afterInsert(M model);

  Mono<Void> afterUpdate(M model);

  Mono<Void> afterDelete(ID id, DeleteContext<M> context);

  record DeleteContext<M>(M previousModel) {}
}
