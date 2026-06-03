package tech.pardus.cache.read;

import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.Identifiable;

public abstract class AbstractCachedViewService<ID, V, M extends Identifiable<ID>>
    implements EntityViewService<ID, V> {

  private final CacheReadStrategy<ID, M> readStrategy;
  private final EntityLoader<ID, M> loader;
  private final Function<M, V> toView;
  private final Function<ID, String> memberIdFn;

  protected AbstractCachedViewService(
      CacheReadStrategy<ID, M> readStrategy,
      EntityLoader<ID, M> loader,
      Function<M, V> toView,
      Function<ID, String> memberIdFn) {
    this.readStrategy = readStrategy;
    this.loader = loader;
    this.toView = toView;
    this.memberIdFn = memberIdFn;
  }

  @Override
  public Mono<V> getById(ID id) {
    if (id == null) {
      return Mono.empty();
    }
    return readStrategy
        .getByMemberId(memberIdFn.apply(id), loader.findById(id))
        .map(toView);
  }

  @Override
  public Flux<V> findAll() {
    return readStrategy
        .getAllIndexed(loader.findAll())
        .flatMapMany(models -> Flux.fromIterable(models.stream().map(toView).toList()));
  }

  protected CacheReadStrategy<ID, M> readStrategy() {
    return readStrategy;
  }

  protected EntityLoader<ID, M> loader() {
    return loader;
  }
}
