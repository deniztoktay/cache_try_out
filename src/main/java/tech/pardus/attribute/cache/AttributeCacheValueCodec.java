package tech.pardus.attribute.cache;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.redis.cache.CacheValueCodec;

@Component
public class AttributeCacheValueCodec implements CacheValueCodec<AttributeModel> {
  private static final String SEP = "\u001f";

  @Override
  public byte[] encode(AttributeModel value) {
    var line =
        String.join(
            SEP,
            nullToEmpty(value.name()),
            nullToEmpty(value.description()),
            nullToEmpty(value.type()),
            value.isGmp() == null ? "" : value.isGmp().toString(),
            value.showToAdmin() == null ? "" : value.showToAdmin().toString(),
            value.showToUser() == null ? "" : value.showToUser().toString());
    return line.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public AttributeModel decode(byte[] bytes, String stringId) {
    var parts = new String(bytes, StandardCharsets.UTF_8).split(SEP, -1);
    if (parts.length < 6) {
      throw new IllegalStateException("Invalid AttributeModel cache payload for id=" + stringId);
    }
    return new AttributeModel(
        Integer.valueOf(stringId),
        emptyToNull(parts[0]),
        emptyToNull(parts[1]),
        emptyToNull(parts[2]),
        parts[3].isEmpty() ? null : Boolean.valueOf(parts[3]),
        parts[4].isEmpty() ? null : Boolean.valueOf(parts[4]),
        parts[5].isEmpty() ? null : Boolean.valueOf(parts[5]));
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String emptyToNull(String value) {
    return value == null || value.isEmpty() ? null : value;
  }
}
