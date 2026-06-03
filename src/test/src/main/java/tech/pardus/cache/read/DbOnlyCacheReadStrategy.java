package tech.pardus.cache.read;

import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.Identifiable;

public class DbOnlyCacheReadStrategy<ID, M extends Identifiable<ID>> implements CacheReadStrategy<ID, M> {

  private final EntityLoader<ID, M> loader;

  public DbOnlyCacheReadStrategy(EntityLoader<ID, M> loader) {
    this.loader = loader;
  }

  @Override
  public Mono<M> getByMemberId(String memberId, Mono<M> databaseFallback) {
    return databaseFallback;
  }

  @Override
  public Mono<List<M>> getAllIndexed(Mono<List<M>> databaseFallback) {
    return databaseFallback;
  }

  @Override
  public Mono<List<M>> loadAllForValidation() {
    return loader.findAll();
  }

  @Override
  public Mono<Optional<M>> findByIdForValidation(ID id) {
    return loader.findById(id).map(Optional::of).defaultIfEmpty(Optional.empty());
  }

  @Override
  public Mono<M> loadForL1Projection(ID id) {
    return loader.findById(id);
  }

  @Override
  public boolean supportsL1() {
    return false;
  }
}
