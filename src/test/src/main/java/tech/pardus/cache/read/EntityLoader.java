package tech.pardus.cache.read;

import java.util.List;
import reactor.core.publisher.Mono;

/** Reactive database read port (source of truth). */
public interface EntityLoader<ID, M> {

  Mono<M> findById(ID id);

  Mono<List<M>> findAll();
}
