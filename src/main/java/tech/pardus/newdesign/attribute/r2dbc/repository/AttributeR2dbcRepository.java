package tech.pardus.newdesign.attribute.r2dbc.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.attribute.r2dbc.entity.AttributeRecord;

public interface AttributeR2dbcRepository extends ReactiveCrudRepository<AttributeRecord, Integer> {

  Mono<AttributeRecord> findByName(String name);
}
