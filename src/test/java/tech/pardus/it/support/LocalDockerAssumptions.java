package tech.pardus.it.support;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

/** Skips tests when local docker-compose services are not running. */
public final class LocalDockerAssumptions {

  private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);

  private LocalDockerAssumptions() {}

  public static void assumeRedisAvailable(ReactiveStringRedisTemplate redis) {
    try {
      Boolean ok =
          redis
              .opsForValue()
              .set("it:probe", "ok", Duration.ofMinutes(1))
              .then(redis.opsForValue().get("it:probe"))
              .map("ok"::equals)
              .defaultIfEmpty(false)
              .block(PROBE_TIMEOUT);
      assumeTrue(
          Boolean.TRUE.equals(ok),
          "Redis not reachable at localhost:6379 (password mysecret). Start: docker compose -f docker/docker-compose.yml up -d redis-master");
    } catch (Exception ex) {
      assumeTrue(
          false,
          "Redis probe failed: "
              + ex.getMessage()
              + ". Start docker-compose redis-master on port 6379.");
    }
  }

  public static void assumeSqlServerAvailable(R2dbcEntityTemplate template) {
    try {
      template.getDatabaseClient().sql("SELECT 1").fetch().one().block(PROBE_TIMEOUT);
    } catch (Exception ex) {
      assumeTrue(
          false,
          "SQL Server probe failed: "
              + ex.getMessage()
              + ". Start docker-compose mssql on port 2023 (database lah).");
    }
  }

  public static void assumeTagTableReadable(R2dbcEntityTemplate template) {
    assumeSqlServerAvailable(template);
    try {
      template
          .getDatabaseClient()
          .sql("SELECT TOP 1 \"TagId\" FROM lah.\"Tag\"")
          .fetch()
          .one()
          .block(PROBE_TIMEOUT);
    } catch (Exception ex) {
      assumeTrue(
          false,
          "lah.Tag not readable: " + ex.getMessage() + ". Ensure schema/table exists in database lah.");
    }
  }
}
