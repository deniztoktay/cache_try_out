package tech.pardus.newdesign.read;

import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Grouped collection reads. Maps to Redis {@code v:{groupId}} and {@code v:n:{groupAlias}}.
 *
 * <p>AttributeSetting: {@code findByAttributeId} → value group, {@code findByReferenceTypeId} →
 * named group.
 */
public interface GroupedCollectionReadStrategy<T> {

  ReadTier tier();

  Mono<List<T>> findByValueGroup(String groupId, Mono<List<T>> databaseFallback);

  Mono<List<T>> findByNamedGroup(String groupAlias, Mono<List<T>> databaseFallback);
}
