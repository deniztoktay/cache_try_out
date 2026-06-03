package tech.pardus.cache.write;

import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.AfterCommitCallback;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.validation.JdbcEntityValidator;
import tech.pardus.redis.cache.Identifiable;

public abstract class AbstractCachedSaveService<E, ID, M extends Identifiable<ID>>
    extends AbstractJdbcSaveService<E> {

  protected final CacheWriteSync<ID, M> cacheWriteSync;
  protected final Function<E, M> toModel;

  protected AbstractCachedSaveService(
      TransactionalSaveOrchestrator orchestrator,
      CacheWriteSync<ID, M> cacheWriteSync,
      Function<E, M> toModel) {
    super(orchestrator);
    this.cacheWriteSync = cacheWriteSync;
    this.toModel = toModel;
  }

  protected AfterCommitCallback afterInsert(E entity) {
    return () -> cacheWriteSync.afterInsert(toModel.apply(entity)).subscribe();
  }

  protected AfterCommitCallback afterUpdate(E entity) {
    return () -> cacheWriteSync.afterUpdate(toModel.apply(entity)).subscribe();
  }

  protected AfterCommitCallback afterDelete(ID id, CacheWriteSync.DeleteContext<M> context) {
    return () -> cacheWriteSync.afterDelete(id, context).subscribe();
  }

  protected Mono<E> insertCached(
      E entity, JdbcEntityValidator<E> validator, Function<E, E> persister) {
    return insert(entity, validator, persister, List.of(afterInsert(entity)));
  }

  protected Mono<E> updateCached(
      E entity, JdbcEntityValidator<E> validator, Function<E, E> persister) {
    return update(entity, validator, persister, List.of(afterUpdate(entity)));
  }
}
