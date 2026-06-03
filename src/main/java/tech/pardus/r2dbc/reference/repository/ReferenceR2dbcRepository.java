package tech.pardus.r2dbc.reference.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.r2dbc.reference.entity.ReferenceRecord;

public interface ReferenceR2dbcRepository extends ReactiveCrudRepository<ReferenceRecord, Integer> {

  Flux<ReferenceRecord> findByReferenceTypeId(Integer referenceTypeId);

  Mono<ReferenceRecord> findByReferenceTypeIdAndValue(Integer referenceTypeId, String value);
}
