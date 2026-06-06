package tech.pardus.newdesign.attributetag.r2dbc.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import tech.pardus.newdesign.attributetag.r2dbc.entity.AttributeTagKey;
import tech.pardus.newdesign.attributetag.r2dbc.entity.AttributeTagRecord;

public interface AttributeTagR2dbcRepository
    extends ReactiveCrudRepository<AttributeTagRecord, AttributeTagKey> {

  Flux<AttributeTagRecord> findByIdAttributeId(Integer attributeId);

  Flux<AttributeTagRecord> findByIdTagId(Integer tagId);
}
