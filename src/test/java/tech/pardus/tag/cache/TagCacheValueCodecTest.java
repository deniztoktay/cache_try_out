package tech.pardus.tag.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tech.pardus.tag.model.TagModel;

class TagCacheValueCodecTest {

  private final TagCacheValueCodec codec = new TagCacheValueCodec();

  @Test
  void encodeDecode_usesStringIdFromKey() {
    var model = new TagModel(42, "Alpha", "type-a", "usage", true);
    var bytes = codec.encode(model);
    var decoded = codec.decode(bytes, model.getStringId());
    assertEquals(model, decoded);
  }

  @Test
  void decode_supportsLegacyPayloadWithEmbeddedId() {
    var legacy =
        "42\u001fAlpha\u001ftype-a\u001fusage\u001ftrue".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var decoded = codec.decode(legacy, "99");
    assertEquals(new TagModel(42, "Alpha", "type-a", "usage", true), decoded);
  }
}
