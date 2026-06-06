package tech.pardus.newdesign.referencetype.r2dbc.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.referencetype.r2dbc.entity.ReferenceTypeRecord;

public interface ReferenceTypeR2dbcRepository
    extends ReactiveCrudRepository<ReferenceTypeRecord, Integer> {

  Mono<ReferenceTypeRecord> findByName(String name);
}
