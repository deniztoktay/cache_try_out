package tech.pardus.newdesign.write;

import reactor.core.publisher.Mono;

/** Post-commit L2 sync for bidirectional grouped id lists ({@code v:*} / {@code v:n:*}). */
public interface GroupedIdListCacheWriteSync {

  Mono<Void> afterLinkInsert(Integer valueGroupId, Integer namedGroupId);

  Mono<Void> afterLinkDelete(Integer valueGroupId, Integer namedGroupId);
}
