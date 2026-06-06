package tech.pardus.newdesign.read;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import reactor.core.publisher.Mono;

/** Single-entity reads at {@code v:{id}} and {@code v:n:{alias}}. */
public interface SingleValueReadStrategy<T> extends L1EntityReadStrategy<T> {

  ReadTier tier();

  Mono<T> findByName(String alias, Mono<T> databaseFallback);

  @Override
  Mono<Optional<T>> findById(String id, Mono<T> databaseFallback);

  @Override
  Mono<List<T>> find(Predicate<T> predicate, Mono<List<T>> databaseFallbackWhenEmpty);
}
