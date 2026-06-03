package tech.pardus.cache.write;

import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.AfterCommitCallback;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.validation.JdbcEntityValidator;

public abstract class AbstractJdbcSaveService<E> {

  protected final TransactionalSaveOrchestrator orchestrator;

  protected AbstractJdbcSaveService(TransactionalSaveOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  protected Mono<E> insert(
      E entity,
      JdbcEntityValidator<E> validator,
      Function<E, E> persister,
      List<AfterCommitCallback> afterCommit) {
    return orchestrator.executeInsert(entity, validator, persister, afterCommit);
  }

  protected Mono<E> update(
      E entity,
      JdbcEntityValidator<E> validator,
      Function<E, E> persister,
      List<AfterCommitCallback> afterCommit) {
    return orchestrator.executeUpdate(entity, validator, persister, afterCommit);
  }

  protected Mono<Void> delete(
      List<tech.pardus.jdbc.ThrowingRunnable> steps,
      List<AfterCommitCallback> afterCommit,
      tech.pardus.jdbc.ThrowingRunnable deleteTask) {
    return orchestrator.executeTransactionalDelete(steps, afterCommit, deleteTask);
  }
}
