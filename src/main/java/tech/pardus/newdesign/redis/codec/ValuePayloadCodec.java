package tech.pardus.newdesign.redis.codec;

/** Serializes one Redis value blob. */
public interface ValuePayloadCodec<T> {

  byte[] encode(T value);

  T decode(byte[] bytes, String cacheKey);
}
