package tech.pardus.format.cache;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import tech.pardus.format.model.FormatTypeModel;
import tech.pardus.redis.cache.CacheValueCodec;

@Component
public class FormatTypeCacheValueCodec implements CacheValueCodec<FormatTypeModel> {
  private static final String SEP = "\u001f";

  @Override
  public byte[] encode(FormatTypeModel value) {
    var line =
        String.join(
            SEP,
            nullToEmpty(value.formatValue()),
            nullToEmpty(value.description()),
            nullToEmpty(value.type()),
            nullToEmpty(value.culture()));
    return line.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public FormatTypeModel decode(byte[] bytes, String stringId) {
    var parts = new String(bytes, StandardCharsets.UTF_8).split(SEP, -1);
    if (parts.length < 4) {
      throw new IllegalStateException("Invalid FormatTypeModel cache payload for id=" + stringId);
    }
    return new FormatTypeModel(
        Integer.valueOf(stringId),
        emptyToNull(parts[0]),
        emptyToNull(parts[1]),
        emptyToNull(parts[2]),
        emptyToNull(parts[3]));
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String emptyToNull(String value) {
    return value == null || value.isEmpty() ? null : value;
  }
}
