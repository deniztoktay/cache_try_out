package tech.pardus.attributetag.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tech.pardus.attributetag.model.AttributeTagModel;

class AttributeTagCacheValueCodecTest {

  private final AttributeTagCacheValueCodec codec = new AttributeTagCacheValueCodec();

  @Test
  void roundTrip() {
    var model = new AttributeTagModel(10, 20, "user1");
    var decoded = codec.decode(codec.encode(model), "10:20");
    assertEquals(model, decoded);
  }
}
