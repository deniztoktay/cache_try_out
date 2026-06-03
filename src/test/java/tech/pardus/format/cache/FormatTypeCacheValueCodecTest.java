package tech.pardus.format.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tech.pardus.format.model.FormatTypeModel;

class FormatTypeCacheValueCodecTest {

  private final FormatTypeCacheValueCodec codec = new FormatTypeCacheValueCodec();

  @Test
  void roundTrip() {
    var model = new FormatTypeModel(1, "yyyy-MM-dd", "date format", "DATE", "en-US");
    var decoded = codec.decode(codec.encode(model), "1");
    assertEquals(model, decoded);
  }
}
