package tech.pardus.jdbc;

/**
 * Like {@link Runnable}, but allows checked exceptions to propagate to the transaction boundary.
 */
@FunctionalInterface
public interface ThrowingRunnable {

  void run() throws Exception;
}
