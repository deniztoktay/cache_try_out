package tech.pardus.redis.api;

import java.util.Collection;
import reactor.core.publisher.Mono;

/** Internal key operations used by initialization swap and grooming (not exposed to service callers). */
public interface RedisKeyMaintenance {

  Mono<Boolean> deleteKey(String key);

  Mono<Long> deleteKeys(Collection<String> keys);

  Mono<Boolean> renameKey(String fromKey, String toKey);
}
