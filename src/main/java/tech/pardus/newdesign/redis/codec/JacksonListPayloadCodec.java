package tech.pardus.newdesign.redis.codec;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** JSON list codec backed by Jackson {@link ObjectMapper}. */
public final class JacksonListPayloadCodec<T> implements ListPayloadCodec<T> {

  private final ObjectMapper objectMapper;
  private final JavaType listJavaType;

  public JacksonListPayloadCodec(ObjectMapper objectMapper, Class<T> elementType) {
    this.objectMapper = objectMapper;
    this.listJavaType =
        objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
  }

  public JacksonListPayloadCodec(ObjectMapper objectMapper, JavaType listJavaType) {
    this.objectMapper = objectMapper;
    this.listJavaType = listJavaType;
  }

  @Override
  public byte[] encode(List<T> values) {
    try {
      return objectMapper.writeValueAsBytes(values == null ? List.of() : values);
    } catch (Exception e) {
      throw new CacheSerializationException("Failed to serialize list payload", e);
    }
  }

  @Override
  public List<T> decode(byte[] bytes, String cacheKey) {
    if (bytes == null || bytes.length == 0) {
      return List.of();
    }
    return deserializeBytes(bytes, cacheKey, listJavaType);
  }

  @SuppressWarnings("unchecked")
  private <V> V deserializeBytes(byte[] bytes, String cacheKey, JavaType valueType) {
    try {
      return (V) objectMapper.readValue(bytes, valueType);
    } catch (Exception e) {
      throw buildDeserializationException(cacheKey, valueType.toString(), e);
    }
  }

  private static CacheSerializationException buildDeserializationException(
      String cacheKey, String valueType, Exception cause) {
    return new CacheSerializationException(
        "Failed to deserialize cache payload for key=%s type=%s".formatted(cacheKey, valueType),
        cause);
  }
}
