package tech.pardus.attributetag.cache;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.attributetag.mapper.AttributeTagMapper;
import tech.pardus.attributetag.model.AttributeTagKeys;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.cache.read.EntityLoader;
import tech.pardus.r2dbc.attribute.entity.AttributeTagKey;
import tech.pardus.r2dbc.attribute.repository.AttributeTagR2dbcRepository;

@Component
public class AttributeTagEntityLoader implements EntityLoader<String, AttributeTagModel> {

  private final AttributeTagR2dbcRepository repository;
  private final AttributeTagMapper mapper;

  public AttributeTagEntityLoader(AttributeTagR2dbcRepository repository, AttributeTagMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Mono<AttributeTagModel> findById(String memberId) {
    var key = AttributeTagKeys.toKey(memberId);
    return repository.findById(key).map(mapper::toModel);
  }

  @Override
  public Mono<List<AttributeTagModel>> findAll() {
    return repository.findAll().map(mapper::toModel).collectList();
  }

  public Mono<List<AttributeTagModel>> findByAttributeId(Integer attributeId) {
    return repository.findByIdAttributeId(attributeId).map(mapper::toModel).collectList();
  }

  public Mono<List<AttributeTagModel>> findByTagId(Integer tagId) {
    return repository.findByIdTagId(tagId).map(mapper::toModel).collectList();
  }

  public Mono<Boolean> existsByKey(Integer attributeId, Integer tagId) {
    return repository
        .findById(new AttributeTagKey(attributeId, tagId))
        .hasElement();
  }
}
