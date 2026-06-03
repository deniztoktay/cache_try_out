package tech.pardus.jdbc;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import tech.pardus.jdbc.validation.JdbcEntityValidator;

/**
 * Runs blocking JPA work on a dedicated executor while exposing a reactive API. All steps run in a
 * single {@link TransactionTemplate} transaction. Register {@link AfterCommitCallback}s for work
 * that must run only after commit (cache refresh, messaging).
 */
@Service
@Slf4j
public class TransactionalSaveOrchestrator {
  private static final Duration VALIDATION_TIMEOUT = Duration.ofSeconds(30);

  private final TransactionTemplate jpaTransactionTemplate;
  private final Scheduler jdbcScheduler;

  public TransactionalSaveOrchestrator(
      TransactionTemplate jpaTransactionTemplate,
      @Qualifier("jdbcThreadPool") Executor jdbcThreadPool) {
    this.jpaTransactionTemplate = jpaTransactionTemplate;
    this.jdbcScheduler = Schedulers.fromExecutor(jdbcThreadPool);
  }

  /** Validates and persists a new entity in one transaction. */
  public <T> Mono<T> executeInsert(
      T entity, JdbcEntityValidator<T> validator, Function<T, T> persister) {
    return executeInsert(entity, validator, persister, List.of());
  }

  public <T> Mono<T> executeInsert(
      T entity,
      JdbcEntityValidator<T> validator,
      Function<T, T> persister,
      List<AfterCommitCallback> afterCommitCallbacks) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(validator, "validator");
    Objects.requireNonNull(persister, "persister");
    return executeTransactionalSave(
        List.of(() -> blockValidation(validator.validateForInsert(entity))),
        afterCommitCallbacks,
        () -> persister.apply(entity));
  }

  /** Validates and persists an updated entity in one transaction. */
  public <T> Mono<T> executeUpdate(
      T entity, JdbcEntityValidator<T> validator, Function<T, T> persister) {
    return executeUpdate(entity, validator, persister, List.of());
  }

  public <T> Mono<T> executeUpdate(
      T entity,
      JdbcEntityValidator<T> validator,
      Function<T, T> persister,
      List<AfterCommitCallback> afterCommitCallbacks) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(validator, "validator");
    Objects.requireNonNull(persister, "persister");
    return executeTransactionalSave(
        List.of(() -> blockValidation(validator.validateForUpdate(entity))),
        afterCommitCallbacks,
        () -> persister.apply(entity));
  }

  public <T> Mono<T> executeTransactionalSave(ThrowingSupplier<T> jpaSaveTask) {
    return executeTransactionalSave(List.of(), List.of(), jpaSaveTask);
  }

  public <T> Mono<T> executeTransactionalSave(
      List<ThrowingRunnable> transactionalSteps, ThrowingSupplier<T> resultSupplier) {
    return executeTransactionalSave(transactionalSteps, List.of(), resultSupplier);
  }

  public <T> Mono<T> executeTransactionalSave(
      List<ThrowingRunnable> transactionalSteps,
      List<AfterCommitCallback> afterCommitCallbacks,
      ThrowingSupplier<T> resultSupplier) {
    Objects.requireNonNull(transactionalSteps, "transactionalSteps");
    Objects.requireNonNull(afterCommitCallbacks, "afterCommitCallbacks");
    Objects.requireNonNull(resultSupplier, "resultSupplier");

    return Mono.fromCallable(() -> runInTransaction(transactionalSteps, afterCommitCallbacks, resultSupplier))
        .subscribeOn(jdbcScheduler)
        .flatMap(Mono::just)
        .onErrorMap(this::unwrapTransactional);
  }

  @SafeVarargs
  public final <T> Mono<T> executeTransactionalSave(
      ThrowingSupplier<T> resultSupplier, ThrowingRunnable... steps) {
    return executeTransactionalSave(Arrays.asList(steps), resultSupplier);
  }

  public Mono<Void> executeTransactionalDelete(ThrowingRunnable deleteTask) {
    return executeTransactionalDelete(List.of(), List.of(), deleteTask);
  }

  public Mono<Void> executeTransactionalDelete(
      List<ThrowingRunnable> transactionalSteps, ThrowingRunnable deleteTask) {
    return executeTransactionalDelete(transactionalSteps, List.of(), deleteTask);
  }

  public Mono<Void> executeTransactionalDelete(
      List<ThrowingRunnable> transactionalSteps,
      List<AfterCommitCallback> afterCommitCallbacks,
      ThrowingRunnable deleteTask) {
    Objects.requireNonNull(transactionalSteps, "transactionalSteps");
    Objects.requireNonNull(afterCommitCallbacks, "afterCommitCallbacks");
    Objects.requireNonNull(deleteTask, "deleteTask");

    return Mono.fromCallable(
            () -> {
              runInTransactionWithoutResult(transactionalSteps, afterCommitCallbacks, deleteTask);
              return Boolean.TRUE;
            })
        .subscribeOn(jdbcScheduler)
        .then()
        .onErrorMap(this::unwrapTransactional);
  }

  @SafeVarargs
  public final Mono<Void> executeTransactionalDelete(
      ThrowingRunnable deleteTask, ThrowingRunnable... steps) {
    return executeTransactionalDelete(Arrays.asList(steps), deleteTask);
  }

  /** @deprecated Use {@link #executeTransactionalSave(ThrowingSupplier)} */
  @Deprecated
  public <T> Mono<T> executeAuditedSave(ThrowingSupplier<T> jpaSaveTask) {
    return executeTransactionalSave(jpaSaveTask);
  }

  /** @deprecated Use {@link #executeTransactionalSave(List, ThrowingSupplier)} */
  @Deprecated
  public <T> Mono<T> executeAuditedSave(
      List<ThrowingRunnable> transactionalSteps, ThrowingSupplier<T> resultSupplier) {
    return executeTransactionalSave(transactionalSteps, resultSupplier);
  }

  /** @deprecated Use {@link #executeTransactionalDelete(ThrowingRunnable)} */
  @Deprecated
  public Mono<Void> executeAuditedDelete(ThrowingRunnable jpaDeleteTask) {
    return executeTransactionalDelete(jpaDeleteTask);
  }

  private <T> T runInTransaction(
      List<ThrowingRunnable> steps,
      List<AfterCommitCallback> afterCommitCallbacks,
      ThrowingSupplier<T> resultSupplier)
      throws Exception {
    return jpaTransactionTemplate.execute(
        status -> {
          try {
            log.debug("Executing JPA transaction ({} step(s) + result)", steps.size());
            for (ThrowingRunnable step : steps) {
              step.run();
            }
            registerAfterCommitCallbacks(afterCommitCallbacks);
            T result = resultSupplier.get();
            if (result == null) {
              throw new IllegalStateException("Transactional result supplier returned null");
            }
            return result;
          } catch (Exception e) {
            status.setRollbackOnly();
            throw propagate(e);
          }
        });
  }

  private void runInTransactionWithoutResult(
      List<ThrowingRunnable> steps,
      List<AfterCommitCallback> afterCommitCallbacks,
      ThrowingRunnable deleteTask)
      throws Exception {
    jpaTransactionTemplate.execute(
        new TransactionCallbackWithoutResult() {
          @Override
          protected void doInTransactionWithoutResult(TransactionStatus status) {
            try {
              log.debug("Executing JPA delete transaction ({} step(s))", steps.size());
              for (ThrowingRunnable step : steps) {
                step.run();
              }
              registerAfterCommitCallbacks(afterCommitCallbacks);
              deleteTask.run();
            } catch (Exception e) {
              status.setRollbackOnly();
              throw propagate(e);
            }
          }
        });
  }

  private void registerAfterCommitCallbacks(List<AfterCommitCallback> callbacks) {
    if (callbacks.isEmpty()) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      log.warn("Transaction synchronization not active; skipping {} afterCommit callback(s)", callbacks.size());
      return;
    }
    for (AfterCommitCallback callback : callbacks) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              try {
                callback.run();
              } catch (Exception ex) {
                log.error("afterCommit callback failed", ex);
              }
            }
          });
    }
  }

  private void blockValidation(reactor.core.publisher.Mono<Void> validation) {
    validation.block(VALIDATION_TIMEOUT);
  }

  private RuntimeException propagate(Exception e) {
    if (e instanceof RuntimeException runtime) {
      return runtime;
    }
    return new RuntimeException(e);
  }

  private Throwable unwrapTransactional(Throwable t) {
    Throwable current = t;
    while (current instanceof RuntimeException && current.getCause() != null) {
      if (current.getCause() == current) {
        break;
      }
      current = current.getCause();
    }
    return current;
  }
}
