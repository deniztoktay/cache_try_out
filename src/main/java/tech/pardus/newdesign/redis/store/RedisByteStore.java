package tech.pardus.newdesign.redis.store;

import java.time.Duration;
import reactor.core.publisher.Mono;

/** Minimal reactive byte store used by the new design Redis layer. */
public interface RedisByteStore {

  Mono<byte[]> get(String key);

  Mono<Void> set(String key, byte[] payload, Duration ttl);

  Mono<Void> delete(String key);
}
