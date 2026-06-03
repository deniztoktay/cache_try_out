package tech.pardus.redis.bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lettuce.core.RedisCommandExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class CacheStartupRunnerSupportTest {

  @Test
  void isNonFatalStartupError_redisNoSuchKey() {
    var ex = new RedisCommandExecutionException("ERR no such key");
    assertTrue(CacheStartupRunnerSupport.isNonFatalStartupError(ex));
  }

  @Test
  void isNonFatalStartupError_databaseFailure() {
    assertFalse(
        CacheStartupRunnerSupport.isNonFatalStartupError(
            new DataAccessResourceFailureException("db down")));
  }
}
