package tech.pardus.newdesign.attributetag.loader;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.attributetag.mapper.AttributeTagMapper;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.newdesign.attributetag.r2dbc.entity.AttributeTagKey;
import tech.pardus.newdesign.attributetag.r2dbc.repository.AttributeTagR2dbcRepository;

@Component
public class AttributeTagEntityLoader {

  private final AttributeTagR2dbcRepository repository;
  private final AttributeTagMapper mapper;

  public AttributeTagEntityLoader(AttributeTagR2dbcRepository repository, AttributeTagMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public Mono<List<Integer>> findTagIdsByAttributeId(Integer attributeId) {
    if (attributeId == null) {
      return Mono.just(List.of());
    }
    return repository
        .findByIdAttributeId(attributeId)
        .map(record -> record.getTagId())
        .collectList();
  }

  public Mono<List<Integer>> findAttributeIdsByTagId(Integer tagId) {
    if (tagId == null) {
      return Mono.just(List.of());
    }
    return repository
        .findByIdTagId(tagId)
        .map(record -> record.getAttributeId())
        .collectList();
  }

  public Mono<AttributeTagModel> findByKey(Integer attributeId, Integer tagId) {
    if (attributeId == null || tagId == null) {
      return Mono.empty();
    }
    return repository.findById(new AttributeTagKey(attributeId, tagId)).map(mapper::toModel);
  }

  public Mono<List<AttributeTagModel>> findAll() {
    return repository.findAll().map(mapper::toModel).collectList();
  }

  public Mono<Boolean> existsByKey(Integer attributeId, Integer tagId) {
    if (attributeId == null || tagId == null) {
      return Mono.just(false);
    }
    return repository.findById(new AttributeTagKey(attributeId, tagId)).hasElement();
  }
}
