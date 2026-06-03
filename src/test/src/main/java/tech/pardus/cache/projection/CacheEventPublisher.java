package tech.pardus.cache.projection;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.pardus.cache.tier.CacheEntityDescriptor;
import tech.pardus.redis.api.RedisStreamBus;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.dto.CacheChangeOperation;

@Slf4j
public class CacheEventPublisher {

  private final RedisStreamBus streamBus;
  private final String streamKey;
  private final CacheEntityDescriptor descriptor;

  public CacheEventPublisher(RedisStreamBus streamBus, CacheEntityDescriptor descriptor) {
    this.streamBus = streamBus;
    this.descriptor = descriptor;
    this.streamKey = CacheKeyLayout.changeStreamKey(descriptor.namespace());
  }

  public Mono<String> publish(CacheChangeOperation operation, List<String> memberIds) {
    var retention =
        descriptor.streamConfig().orElseThrow().getRetentionTtl();
    return streamBus
        .xadd(streamKey, CacheChangeStreamMessage.toFields(operation, memberIds), retention)
        .doOnSuccess(
            id ->
                log.debug(
                    "Published {} cache change op={} ids={} messageId={}",
                    descriptor.entityName(),
                    operation,
                    memberIds,
                    id));
  }
}
