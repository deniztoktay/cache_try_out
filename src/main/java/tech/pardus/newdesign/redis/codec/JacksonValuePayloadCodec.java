package tech.pardus.newdesign.redis.codec;

import com.fasterxml.jackson.databind.ObjectMapper;

/** JSON value codec backed by Jackson {@link ObjectMapper}. */
public final class JacksonValuePayloadCodec<T> implements ValuePayloadCodec<T> {

  private final ObjectMapper objectMapper;
  private final Class<T> valueType;

  public JacksonValuePayloadCodec(ObjectMapper objectMapper, Class<T> valueType) {
    this.objectMapper = objectMapper;
    this.valueType = valueType;
  }

  @Override
  public byte[] encode(T value) {
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (Exception e) {
      throw new CacheSerializationException("Failed to serialize value payload", e);
    }
  }

  @Override
  public T decode(byte[] bytes, String cacheKey) {
    try {
      return objectMapper.readValue(bytes, valueType);
    } catch (Exception e) {
      throw buildDeserializationException(cacheKey, valueType.getName(), e);
    }
  }

  private static CacheSerializationException buildDeserializationException(
      String cacheKey, String valueType, Exception cause) {
    return new CacheSerializationException(
        "Failed to deserialize cache payload for key=%s type=%s".formatted(cacheKey, valueType),
        cause);
  }
}
