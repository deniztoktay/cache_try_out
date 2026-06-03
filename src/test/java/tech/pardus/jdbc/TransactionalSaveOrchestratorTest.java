package tech.pardus.jdbc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.test.StepVerifier;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.jdbc.validation.JdbcValidationException;

class TransactionalSaveOrchestratorTest {

  @Test
  void unwrapsValidationExceptionFromTransaction() {
    PlatformTransactionManager manager =
        new PlatformTransactionManager() {
          @Override
          public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
          }

          @Override
          public void commit(TransactionStatus status) {}

          @Override
          public void rollback(TransactionStatus status) {}
        };

    var orchestrator =
        new TransactionalSaveOrchestrator(
            new TransactionTemplate(manager), Executors.newSingleThreadExecutor());

    StepVerifier.create(
            orchestrator.executeTransactionalSave(
                List.of(
                    () -> {
                      throw JdbcValidationException.badRequest("TEST", "bad");
                    }),
                () -> "ok"))
        .expectErrorSatisfies(err -> assertInstanceOf(PRuntimeException.class, unwrap(err)))
        .verify();
  }

  private static Throwable unwrap(Throwable t) {
    while (t.getCause() != null && t.getCause() != t) {
      t = t.getCause();
    }
    return t;
  }
}
