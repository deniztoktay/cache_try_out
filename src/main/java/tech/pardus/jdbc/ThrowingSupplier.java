package tech.pardus.jdbc;

/**
 * Like {@link java.util.function.Supplier}, but allows checked exceptions from the transactional
 * work.
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {

  T get() throws Exception;
}
