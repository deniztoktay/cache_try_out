package tech.pardus.redis.api;

import java.time.Duration;
import java.util.Map;
import reactor.core.publisher.Mono;

/** Byte/hash value access; all writes require a positive TTL. */
public interface RedisValueStore {

  Mono<Boolean> setBytes(String key, byte[] value, Duration ttl);

  Mono<byte[]> getBytes(String key);

  Mono<Boolean> setHash(String key, Map<String, byte[]> fields, Duration ttl);

  Mono<Map<String, byte[]>> getHash(String key);
}
