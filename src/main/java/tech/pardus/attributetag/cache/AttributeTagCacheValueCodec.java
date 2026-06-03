package tech.pardus.attributetag.cache;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import tech.pardus.attributetag.model.AttributeTagKeys;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.redis.cache.CacheValueCodec;

@Component
public class AttributeTagCacheValueCodec implements CacheValueCodec<AttributeTagModel> {
  private static final String SEP = "\u001f";

  @Override
  public byte[] encode(AttributeTagModel value) {
    var line =
        String.join(
            SEP,
            String.valueOf(value.attributeId()),
            String.valueOf(value.tagId()),
            value.userId() == null ? "" : value.userId());
    return line.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public AttributeTagModel decode(byte[] bytes, String stringId) {
    var parts = new String(bytes, StandardCharsets.UTF_8).split(SEP, -1);
    if (parts.length < 2) {
      throw new IllegalStateException("Invalid AttributeTagModel cache payload for id=" + stringId);
    }
    var key = AttributeTagKeys.toKey(stringId);
    return new AttributeTagModel(
        key.attributeId(),
        key.tagId(),
        parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null);
  }
}
