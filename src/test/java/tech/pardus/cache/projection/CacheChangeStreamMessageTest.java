package tech.pardus.cache.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import tech.pardus.redis.dto.CacheChangeOperation;
import tech.pardus.redis.dto.StreamMessage;

class CacheChangeStreamMessageTest {

  @Test
  void roundTripFields() {
    var fields = CacheChangeStreamMessage.toFields(CacheChangeOperation.UPDATE, java.util.List.of("7", "9"));
    var parsed = CacheChangeStreamMessage.from(new StreamMessage("1-0", fields));

    assertEquals(CacheChangeOperation.UPDATE, parsed.operation());
    assertEquals(java.util.List.of("7", "9"), parsed.ids());
  }

  @Test
  void rejectsMissingIds() {
    assertThrows(
        IllegalArgumentException.class,
        () -> CacheChangeStreamMessage.from(new StreamMessage("1-0", Map.of("op", "DELETE"))));
  }
}
