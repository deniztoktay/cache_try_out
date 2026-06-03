package tech.pardus.it;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.test.StepVerifier;
import tech.pardus.it.support.AbstractLocalDockerIT;
import tech.pardus.it.support.LocalDockerAssumptions;

class LocalDockerRedisIT extends AbstractLocalDockerIT {

  @Autowired private ReactiveStringRedisTemplate redis;

  @BeforeEach
  void requireRedis() {
    LocalDockerAssumptions.assumeRedisAvailable(redis);
  }

  @Test
  void redis_setAndGet_withDockerComposePassword() {
    var key = "it:cache_try_out:ping";

    StepVerifier.create(
            redis
                .opsForValue()
                .set(key, "pong", Duration.ofMinutes(5))
                .then(redis.opsForValue().get(key))
                .flatMap(
                    value ->
                        redis
                            .delete(key)
                            .thenReturn(value)))
        .expectNext("pong")
        .verifyComplete();
  }
}
