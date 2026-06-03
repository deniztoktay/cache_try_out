package tech.pardus.jdbc.attributetag.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.pardus.attributetag.mapper.AttributeTagMapper;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.cache.write.AbstractCachedSaveService;
import tech.pardus.cache.write.CacheWriteSync;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.attribute.entity.AttributeTagEntity;
import tech.pardus.jdbc.attribute.entity.compositeid.AttributeTagId;
import tech.pardus.jdbc.attribute.repository.AttributeTagJpaRepository;
import tech.pardus.jdbc.attributetag.validation.AttributeTagEntityValidator;

@Service
public class AttributeTagWriteService
    extends AbstractCachedSaveService<AttributeTagEntity, String, AttributeTagModel> {

  private final AttributeTagEntityValidator validator;
  private final AttributeTagJpaRepository repository;
  private final AttributeTagMapper mapper;

  public AttributeTagWriteService(
      TransactionalSaveOrchestrator orchestrator,
      AttributeTagEntityValidator validator,
      AttributeTagJpaRepository repository,
      AttributeTagMapper mapper,
      @Qualifier("attributeTagCacheWriteSync")
          CacheWriteSync<String, AttributeTagModel> attributeTagCacheWriteSync) {
    super(orchestrator, attributeTagCacheWriteSync, mapper::toModel);
    this.validator = validator;
    this.repository = repository;
    this.mapper = mapper;
  }

  public Mono<AttributeTagEntity> create(AttributeTagEntity entity) {
    return insertCached(entity, validator, repository::save);
  }

  public Mono<Void> delete(Integer attributeId, Integer tagId) {
    var linkId = new AttributeTagId(attributeId, tagId);
    return Mono.fromCallable(() -> repository.findById(linkId))
        .flatMap(
            opt ->
                opt.map(Mono::just)
                    .orElseGet(
                        () ->
                            Mono.error(
                                tech.pardus.jdbc.validation.JdbcValidationException.badRequest(
                                    "ATTRIBUTE_TAG_NOT_FOUND",
                                    "AttributeTag not found for attributeId=%s tagId=%s"
                                        .formatted(attributeId, tagId)))))
        .flatMap(
            existing -> {
              var model = mapper.toModel(existing);
              var context = new CacheWriteSync.DeleteContext<>(model, null);
              return delete(
                  List.of(() -> validator.validateLinkExists(attributeId, tagId).block()),
                  List.of(afterDelete(model.getStringId(), context)),
                  () -> repository.deleteById(linkId));
            });
  }
}
