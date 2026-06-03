package tech.pardus.cache.l1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.redis.cache.CacheValueCodec;
import tech.pardus.redis.cache.Identifiable;
import tech.pardus.redis.cache.ReactiveCacheReader;

/**
 * L1 (Caffeine) → L2 (Redis via {@link ReactiveCacheReader}) → database fallback.
 */
@Slf4j
public class TieredReactiveCacheReader<T extends Identifiable<?>> {
  private final ResizableL1Cache<String, T> l1;
  private final ReactiveCacheReader<T> l2;
  private final CacheValueCodec<T> codec;
  private volatile boolean l1FullyWarm;

  public TieredReactiveCacheReader(
      ResizableL1Cache<String, T> l1, ReactiveCacheReader<T> l2, CacheValueCodec<T> codec) {
    this.l1 = l1;
    this.l2 = l2;
    this.codec = codec;
  }

  public void markFullyWarm() {
    this.l1FullyWarm = true;
  }

  public Mono<T> getByMemberId(String memberId, Mono<T> databaseFallback) {
    Optional<T> fromL1 = l1.get(memberId);
    if (fromL1.isPresent()) {
      return Mono.just(fromL1.get());
    }
    return l2.getByMemberId(memberId, databaseFallback).doOnNext(value -> l1.put(memberId, value));
  }

  /** Primary-index L1 entries only (excludes name-alias keys). Empty when L1 is not warm or has no data. */
  public Mono<List<T>> tryGetAllFromL1() {
    if (!l1FullyWarm || l1.snapshot().isEmpty()) {
      return Mono.empty();
    }
    var fromL1 =
        l1.snapshot().entrySet().stream()
            .filter(e -> !e.getKey().startsWith("n:"))
            .map(java.util.Map.Entry::getValue)
            .toList();
    if (fromL1.isEmpty()) {
      return Mono.empty();
    }
    return Mono.just(new ArrayList<>(fromL1));
  }

  public Mono<List<T>> getAllIndexed(Mono<List<T>> databaseFallback) {
    return tryGetAllFromL1()
        .switchIfEmpty(
            l2.getAllIndexed(databaseFallback)
                .doOnNext(
                    list -> {
                      list.forEach(item -> l1.put(codec.stringId(item), item));
                      l1FullyWarm = true;
                    }));
  }

  public void warmL1(List<T> items) {
    items.forEach(item -> l1.put(codec.stringId(item), item));
    markFullyWarm();
  }
}
