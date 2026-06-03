package tech.pardus.redis.bootstrap;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.r2dbc.BadSqlGrammarException;
import reactor.core.publisher.Mono;

@Slf4j
public final class CacheStartupRunnerSupport {

  private CacheStartupRunnerSupport() {}

  /**
   * Runs cache coordination at startup. Redis/cache failures are logged and ignored so the app can
   * still serve traffic (reads fall back to DB). Only database connectivity failures propagate.
   */
  public static void runBestEffort(String cacheName, Mono<Void> coordination, Duration timeout) {
    try {
      coordination
          .doOnError(ex -> log.warn("{} cache startup failed: {}", cacheName, ex.getMessage()))
          .onErrorResume(CacheStartupRunnerSupport::isNonFatalStartupError, ex -> Mono.empty())
          .block(timeout);
      log.info("{} cache startup coordination finished on this pod", cacheName);
    } catch (Exception ex) {
      if (isNonFatalStartupError(ex)) {
        log.warn(
            "{} cache startup failed (application continues without warm cache): {}",
            cacheName,
            ex.getMessage());
        return;
      }
      throw ex;
    }
  }

  static boolean isNonFatalStartupError(Throwable ex) {
    if (isDatabaseFailure(ex)) {
      return false;
    }
    return true;
  }

  private static boolean isDatabaseFailure(Throwable ex) {
    var current = ex;
    while (current != null) {
      if (current instanceof DataAccessException) {
        return true;
      }
      if (current instanceof BadSqlGrammarException) {
        return true;
      }
      var name = current.getClass().getName();
      if (name.contains("R2dbc") && name.contains("Exception")) {
        return true;
      }
      if (name.contains("SQLException") || name.contains("SQLServerException")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
