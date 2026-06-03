package tech.pardus.redis.cache;

/**
 * Encodes/decodes cache values for {@link tech.pardus.redis.api.RedisValueStore} (L2). L1 caches
 * hold live {@code T} instances and only use {@link #stringId(Identifiable)} for keying.
 */
public interface CacheValueCodec<T extends Identifiable<?>> {

  byte[] encode(T value);

  /** Decodes a value; {@code stringId} is the Redis key suffix / index member (from {@link #stringId}). */
  T decode(byte[] bytes, String stringId);

  /** Redis key suffix and index member id; defaults to {@link Identifiable#getStringId()}. */
  default String stringId(T value) {
    return value.getStringId();
  }
}
