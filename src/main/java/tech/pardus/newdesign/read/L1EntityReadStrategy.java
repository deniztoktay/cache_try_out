package tech.pardus.newdesign.read;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import reactor.core.publisher.Mono;

/** L1 id map reads: by id or predicate (no parent-key search on L1). */
public interface L1EntityReadStrategy<T> {

  Mono<Optional<T>> findById(String id, Mono<T> databaseFallback);

  Mono<List<T>> find(Predicate<T> predicate, Mono<List<T>> databaseFallbackWhenEmpty);
}
