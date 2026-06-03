package tech.pardus.tag.cache;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import tech.pardus.redis.cache.CacheValueCodec;
import tech.pardus.tag.model.TagModel;

/** Compact delimiter encoding for {@link TagModel} Redis payloads (id stored in key, not in payload). */
@Component
public class TagCacheValueCodec implements CacheValueCodec<TagModel> {
  private static final String SEP = "\u001f";

  @Override
  public byte[] encode(TagModel value) {
    var line =
        String.join(
            SEP,
            nullToEmpty(value.name()),
            nullToEmpty(value.type()),
            nullToEmpty(value.usageType()),
            value.canUserAssign() == null ? "" : value.canUserAssign().toString());
    return line.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public TagModel decode(byte[] bytes, String stringId) {
    var parts = new String(bytes, StandardCharsets.UTF_8).split(SEP, -1);
    if (parts.length >= 5) {
      return decodeLegacy(parts);
    }
    if (parts.length < 4) {
      throw new IllegalStateException("Invalid TagModel cache payload for id=" + stringId);
    }
    return new TagModel(
        Integer.valueOf(stringId),
        emptyToNull(parts[0]),
        emptyToNull(parts[1]),
        emptyToNull(parts[2]),
        parts[3].isEmpty() ? null : Boolean.valueOf(parts[3]));
  }

  private static TagModel decodeLegacy(String[] parts) {
    return new TagModel(
        Integer.valueOf(parts[0]),
        emptyToNull(parts[1]),
        emptyToNull(parts[2]),
        emptyToNull(parts[3]),
        parts[4].isEmpty() ? null : Boolean.valueOf(parts[4]));
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String emptyToNull(String value) {
    return value == null || value.isEmpty() ? null : value;
  }
}
