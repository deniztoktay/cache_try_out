package tech.pardus.cache.write;

import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.Identifiable;

public class NoOpCacheWriteSync<ID, M extends Identifiable<ID>> implements CacheWriteSync<ID, M> {

  @Override
  public Mono<Void> afterInsert(M model) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> afterUpdate(M model) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> afterDelete(ID id, DeleteContext<M> context) {
    return Mono.empty();
  }
}
