package tech.pardus.cache.read;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import tech.pardus.redis.cache.Identifiable;

public abstract class AbstractValidationView<ID, V, M extends Identifiable<ID>>
    implements ValidationEntityView<ID, V> {

  private final CacheReadStrategy<ID, M> readStrategy;
  private final Function<M, V> toView;
  private final Duration blockTimeout;

  protected AbstractValidationView(
      CacheReadStrategy<ID, M> readStrategy, Function<M, V> toView, Duration blockTimeout) {
    this.readStrategy = readStrategy;
    this.toView = toView;
    this.blockTimeout = blockTimeout;
  }

  @Override
  public List<V> findAllForValidation() {
    return readStrategy
        .loadAllForValidation()
        .map(models -> models.stream().map(toView).toList())
        .block(blockTimeout);
  }

  @Override
  public Optional<V> findByIdForValidation(ID id) {
    if (id == null) {
      return Optional.empty();
    }
    return readStrategy
        .findByIdForValidation(id)
        .map(opt -> opt.map(toView))
        .blockOptional(blockTimeout)
        .flatMap(o -> o);
  }

  protected CacheReadStrategy<ID, M> readStrategy() {
    return readStrategy;
  }
}
