package tech.pardus.newdesign.redis.codec;

import java.util.List;

/** Serializes a list stored under one Redis key. */
public interface ListPayloadCodec<T> {

  byte[] encode(List<T> values);

  List<T> decode(byte[] bytes, String cacheKey);
}
