package tech.pardus.jdbc;

/** Runs after a successful transaction commit (e.g. cache invalidation, domain events). */
@FunctionalInterface
public interface AfterCommitCallback {

  void run() throws Exception;
}
