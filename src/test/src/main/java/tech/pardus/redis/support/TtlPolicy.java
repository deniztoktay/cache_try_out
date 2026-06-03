package tech.pardus.redis.support;

import java.time.Duration;
import java.util.Objects;
import tech.pardus.exception.PRuntimeException;
import org.springframework.http.HttpStatus;

/** Validates that cache/stream write operations always receive a positive TTL. */
public final class TtlPolicy {

  private TtlPolicy() {}

  public static Duration requirePositive(Duration ttl, String operation) {
    if (Objects.isNull(ttl) || ttl.isZero() || ttl.isNegative()) {
      throw PRuntimeException.builder()
          .condition("TTL_REQUIRED")
          .status(HttpStatus.BAD_REQUEST)
          .type("CACHE_CONFIGURATION_ERROR")
          .title("TTL required")
          .detail("Operation '" + operation + "' requires a positive Duration TTL")
          .build();
    }
    return ttl;
  }
}
