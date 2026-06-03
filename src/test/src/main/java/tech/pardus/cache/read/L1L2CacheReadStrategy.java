package tech.pardus.cache.read;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.cache.l2.L2SingleValueLoader;
import tech.pardus.redis.cache.CacheNamespace;
import tech.pardus.redis.cache.Identifiable;
import tech.pardus.redis.coordination.CacheL2ReadyMarker;

@Slf4j
public class L1L2CacheReadStrategy<ID, M extends Identifiable<ID>> implements CacheReadStrategy<ID, M> {

  private final TieredReactiveCacheReader<M> tieredReader;
  private final ResizableL1Cache<String, M> l1Cache;
  private final L2SingleValueLoader<M> l2Loader;
  private final EntityLoader<ID, M> loader;
  private final CacheL2ReadyMarker l2ReadyMarker;
  private final CacheNamespace namespace;

  public L1L2CacheReadStrategy(
      TieredReactiveCacheReader<M> tieredReader,
      ResizableL1Cache<String, M> l1Cache,
      L2SingleValueLoader<M> l2Loader,
      EntityLoader<ID, M> loader,
      CacheL2ReadyMarker l2ReadyMarker,
      CacheNamespace namespace) {
    this.tieredReader = tieredReader;
    this.l1Cache = l1Cache;
    this.l2Loader = l2Loader;
    this.loader = loader;
    this.l2ReadyMarker = l2ReadyMarker;
    this.namespace = namespace;
  }

  @Override
  public Mono<M> getByMemberId(String memberId, Mono<M> databaseFallback) {
    return tieredReader.getByMemberId(memberId, databaseFallback);
  }

  @Override
  public Mono<List<M>> getAllIndexed(Mono<List<M>> databaseFallback) {
    return tieredReader.getAllIndexed(databaseFallback);
  }

  @Override
  public Mono<List<M>> loadAllForValidation() {
    return l2ReadyMarker
        .isReady(namespace)
        .flatMap(
            ready -> {
              if (!ready) {
                log.debug("L2 not ready; validation loading from DB");
                return loader.findAll();
              }
              return tieredReader
                  .tryGetAllFromL1()
                  .onErrorResume(ex -> Mono.empty())
                  .switchIfEmpty(l2Loader.loadAllIndexed())
                  .onErrorResume(
                      ex -> {
                        log.warn("L2 validation read-all failed, falling back to DB", ex);
                        return Mono.empty();
                      })
                  .switchIfEmpty(loader.findAll());
            });
  }

  @Override
  public Mono<Optional<M>> findByIdForValidation(ID id) {
    if (id == null) {
      return Mono.just(Optional.empty());
    }
    var memberId = String.valueOf(id);
    Mono<Optional<M>> fromDatabase =
        loader.findById(id).map(Optional::of).defaultIfEmpty(Optional.empty());

    return l2ReadyMarker
        .isReady(namespace)
        .flatMap(
            ready -> {
              if (!ready) {
                return fromDatabase;
              }
              return Mono.defer(
                      () ->
                          l1Cache
                              .get(memberId)
                              .map(model -> Mono.just(Optional.of(model)))
                              .orElse(Mono.empty()))
                  .onErrorResume(ex -> Mono.empty())
                  .switchIfEmpty(l2Loader.loadByMemberId(memberId))
                  .onErrorResume(ex -> Mono.empty())
                  .switchIfEmpty(fromDatabase);
            });
  }

  @Override
  public Mono<M> loadForL1Projection(ID id) {
    if (id == null) {
      return Mono.empty();
    }
    var memberId = String.valueOf(id);
    return l2Loader
        .loadByMemberId(memberId)
        .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()))
        .onErrorResume(
            ex -> {
              log.warn("L2 read failed for L1 projection id={}, loading DB", id, ex);
              return Mono.empty();
            })
        .switchIfEmpty(loader.findById(id));
  }

  @Override
  public boolean supportsL1() {
    return true;
  }

  public ResizableL1Cache<String, M> l1Cache() {
    return l1Cache;
  }

  public TieredReactiveCacheReader<M> tieredReader() {
    return tieredReader;
  }
}
