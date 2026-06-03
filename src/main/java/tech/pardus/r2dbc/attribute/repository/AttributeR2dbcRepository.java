package tech.pardus.r2dbc.attribute.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import tech.pardus.r2dbc.attribute.entity.AttributeRecord;

public interface AttributeR2dbcRepository extends ReactiveCrudRepository<AttributeRecord, Integer> {

  Mono<AttributeRecord> findByName(String name);
}
