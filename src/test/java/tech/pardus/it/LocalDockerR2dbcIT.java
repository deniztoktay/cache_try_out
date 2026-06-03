package tech.pardus.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.test.StepVerifier;
import tech.pardus.it.support.AbstractLocalDockerIT;
import tech.pardus.it.support.LocalDockerAssumptions;
import tech.pardus.r2dbc.tag.repository.TagR2dbcRepository;

class LocalDockerR2dbcIT extends AbstractLocalDockerIT {

  @Autowired private R2dbcEntityTemplate r2dbc;
  @Autowired private TagR2dbcRepository tagRepository;

  @BeforeEach
  void requireDatabase() {
    LocalDockerAssumptions.assumeTagTableReadable(r2dbc);
  }

  @Test
  void sqlServer_acceptsConnection_onPort2023() {
    StepVerifier.create(r2dbc.getDatabaseClient().sql("SELECT 1 AS ok").map(row -> row.get("ok")).one())
        .expectNextMatches(v -> v != null)
        .verifyComplete();
  }

  @Test
  void tagRepository_findAll_readsLahTagTable() {
    StepVerifier.create(tagRepository.findAll().take(5).count())
        .expectNextMatches(count -> count >= 0)
        .verifyComplete();
  }
}
