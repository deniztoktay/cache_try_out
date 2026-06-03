package tech.pardus.r2dbc.attribute.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import tech.pardus.r2dbc.attribute.entity.AttributeTagKey;
import tech.pardus.r2dbc.attribute.entity.AttributeTagRecord;

public interface AttributeTagR2dbcRepository
    extends ReactiveCrudRepository<AttributeTagRecord, AttributeTagKey> {

  Flux<AttributeTagRecord> findByIdAttributeId(Integer attributeId);

  Flux<AttributeTagRecord> findByIdTagId(Integer tagId);
}
