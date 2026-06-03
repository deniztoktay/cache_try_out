package tech.pardus.cache.projection;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tech.pardus.redis.dto.CacheChangeOperation;
import tech.pardus.redis.dto.StreamMessage;

/** Standard change-stream payload: {@code op}, {@code ids} (comma-separated member ids). */
public record CacheChangeStreamMessage(CacheChangeOperation operation, List<String> ids) {

  public static final String FIELD_OP = "op";
  public static final String FIELD_IDS = "ids";

  public static Map<String, String> toFields(CacheChangeOperation operation, List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new IllegalArgumentException("ids must not be empty");
    }
    return Map.of(FIELD_OP, operation.name(), FIELD_IDS, String.join(",", ids));
  }

  public static CacheChangeStreamMessage from(StreamMessage message) {
    var fields = message.fields();
    return new CacheChangeStreamMessage(
        CacheChangeOperation.parse(fields.get(FIELD_OP)), parseIds(fields.get(FIELD_IDS)));
  }

  private static List<String> parseIds(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("ids field is required");
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }
}
