package tech.pardus.cache.write;

import java.util.List;
import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.Identifiable;

/** Post-commit cache synchronization for a tier. */
public interface CacheWriteSync<ID, M extends Identifiable<ID>> {

  Mono<Void> afterInsert(M model);

  Mono<Void> afterUpdate(M model);

  Mono<Void> afterDelete(ID id, DeleteContext<M> context);

  record DeleteContext<M>(M previousModel, String aliasMemberIdOrNull) {}
}
