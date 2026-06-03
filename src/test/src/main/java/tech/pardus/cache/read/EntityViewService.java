package tech.pardus.cache.read;

import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** API-facing read service for a cached entity. */
public interface EntityViewService<ID, V> {

  Mono<V> getById(ID id);

  Flux<V> findAll();

  default Flux<V> streamAll() {
    return findAll();
  }
}
