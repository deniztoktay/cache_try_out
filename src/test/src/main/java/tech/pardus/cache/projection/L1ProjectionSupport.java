package tech.pardus.cache.projection;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.redis.cache.Identifiable;
import tech.pardus.redis.dto.CacheChangeOperation;

@Slf4j
public final class L1ProjectionSupport {

  private L1ProjectionSupport() {}

  public static <ID, M extends Identifiable<ID>> void applyChange(
      CacheChangeStreamMessage message,
      ResizableL1Cache<String, M> l1,
      Function<String, ID> idParser,
      Function<ID, Mono<M>> projectionLoader,
      Function<M, List<String>> l1KeyIds,
      Duration blockTimeout) {

    switch (message.operation()) {
      case DELETE ->
          message.ids().forEach(memberId -> evict(l1, idParser.apply(memberId), l1KeyIds));
      case INSERT, UPDATE ->
          message
              .ids()
              .forEach(
                  memberId ->
                      upsert(l1, idParser.apply(memberId), projectionLoader, l1KeyIds, blockTimeout));
      default -> log.warn("Unhandled cache operation {}", message.operation());
    }
  }

  private static <ID, M extends Identifiable<ID>> void evict(
      ResizableL1Cache<String, M> l1, ID id, Function<M, List<String>> l1KeyIds) {
    var memberId = String.valueOf(id);
    l1.get(memberId)
        .ifPresent(model -> l1KeyIds.apply(model).forEach(l1::invalidate));
    l1.invalidate(memberId);
    log.debug("L1 evicted id={}", id);
  }

  private static <ID, M extends Identifiable<ID>> void upsert(
      ResizableL1Cache<String, M> l1,
      ID id,
      Function<ID, Mono<M>> projectionLoader,
      Function<M, List<String>> l1KeyIds,
      Duration blockTimeout) {

    projectionLoader
        .apply(id)
        .doOnNext(
            model -> {
              l1KeyIds.apply(model).forEach(key -> l1.put(key, model));
              log.debug("L1 upserted id={}", id);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  evict(l1, id, l1KeyIds);
                  return Mono.empty();
                }))
        .block(blockTimeout);
  }
}
