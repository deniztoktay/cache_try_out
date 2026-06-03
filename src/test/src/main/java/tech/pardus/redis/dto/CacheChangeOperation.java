package tech.pardus.redis.dto;

/** Cache projection change emitted on the Redis change stream. */
public enum CacheChangeOperation {
  INSERT,
  UPDATE,
  DELETE;

  public static CacheChangeOperation parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("operation is required");
    }
    return CacheChangeOperation.valueOf(raw.trim().toUpperCase());
  }
}
