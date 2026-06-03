package tech.pardus.attribute.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tech.pardus.attribute.model.AttributeModel;

class AttributeCacheValueCodecTest {

  private final AttributeCacheValueCodec codec = new AttributeCacheValueCodec();

  @Test
  void roundTrip() {
    var model = new AttributeModel(3, "Size", "desc", "NUMBER", true, false, true);
    var bytes = codec.encode(model);
    var decoded = codec.decode(bytes, "3");

    assertEquals(model, decoded);
  }
}
