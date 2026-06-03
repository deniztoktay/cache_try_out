package tech.pardus.cache.read;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.Identifiable;
import tech.pardus.redis.cache.ReactiveCacheReader;

@Slf4j
public class L2OnlyCacheReadStrategy<ID, M extends Identifiable<ID>> implements CacheReadStrategy<ID, M> {

  private final ReactiveCacheReader<M> l2Reader;
  private final EntityLoader<ID, M> loader;

  public L2OnlyCacheReadStrategy(ReactiveCacheReader<M> l2Reader, EntityLoader<ID, M> loader) {
    this.l2Reader = l2Reader;
    this.loader = loader;
  }

  @Override
  public Mono<M> getByMemberId(String memberId, Mono<M> databaseFallback) {
    return l2Reader.getByMemberId(memberId, databaseFallback);
  }

  @Override
  public Mono<List<M>> getAllIndexed(Mono<List<M>> databaseFallback) {
    return l2Reader.getAllIndexed(databaseFallback);
  }

  @Override
  public Mono<List<M>> loadAllForValidation() {
    return l2Reader
        .getAllIndexed(loader.findAll())
        .onErrorResume(
            ex -> {
              log.warn("L2 validation read-all failed, falling back to DB", ex);
              return loader.findAll();
            });
  }

  @Override
  public Mono<Optional<M>> findByIdForValidation(ID id) {
    var memberId = String.valueOf(id);
    return l2Reader
        .getByMemberId(memberId, loader.findById(id))
        .map(Optional::of)
        .defaultIfEmpty(Optional.empty())
        .onErrorResume(
            ex -> {
              log.warn("L2 validation read failed for id={}, falling back to DB", id, ex);
              return loader.findById(id).map(Optional::of).defaultIfEmpty(Optional.empty());
            });
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
