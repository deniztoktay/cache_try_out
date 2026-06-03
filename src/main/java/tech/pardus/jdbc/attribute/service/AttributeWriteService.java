package tech.pardus.jdbc.attribute.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.pardus.attribute.mapper.AttributeMapper;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.cache.write.AbstractCachedSaveService;
import tech.pardus.cache.write.CacheWriteSync;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.attribute.entity.AttributeEntity;
import tech.pardus.jdbc.attribute.repository.AttributesJpaRepository;
import tech.pardus.jdbc.attribute.validation.AttributeEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;

@Service
public class AttributeWriteService
    extends AbstractCachedSaveService<AttributeEntity, Integer, AttributeModel> {

  private final AttributeEntityValidator validator;
  private final AttributesJpaRepository repository;
  private final AttributeMapper attributeMapper;

  public AttributeWriteService(
      TransactionalSaveOrchestrator orchestrator,
      AttributeEntityValidator validator,
      AttributesJpaRepository repository,
      AttributeMapper attributeMapper,
      @Qualifier("attributeCacheWriteSync") CacheWriteSync<Integer, AttributeModel> attributeCacheWriteSync) {
    super(orchestrator, attributeCacheWriteSync, attributeMapper::toModel);
    this.validator = validator;
    this.repository = repository;
    this.attributeMapper = attributeMapper;
  }

  public Mono<AttributeEntity> create(AttributeEntity entity) {
    AttributeEntityValidator.applyInsertDefaults(entity);
    return insertCached(entity, validator, repository::save);
  }

  public Mono<AttributeEntity> update(AttributeEntity entity) {
    return updateCached(entity, validator, repository::save);
  }

  public Mono<Void> deleteById(Integer id) {
    return Mono.fromCallable(() -> repository.findById(id))
        .flatMap(
            opt ->
                opt.map(Mono::just)
                    .orElseGet(
                        () ->
                            Mono.error(
                                JdbcValidationException.badRequest(
                                    "ATTRIBUTE_NOT_FOUND",
                                    "Attribute with id %s does not exist".formatted(id)))))
        .flatMap(
            existing -> {
              var stub = AttributeEntity.builder().id(id).build();
              var context = deleteContext(existing);
              return delete(
                  List.of(() -> validator.validateFk(stub).block()),
                  List.of(afterDelete(id, context)),
                  () -> repository.deleteById(id));
            });
  }

  private CacheWriteSync.DeleteContext<AttributeModel> deleteContext(AttributeEntity existing) {
    var model = attributeMapper.toModel(existing);
    var alias =
        model.name() != null && !model.name().isBlank() ? model.nameAliasStringId() : null;
    return new CacheWriteSync.DeleteContext<>(model, alias);
  }
}
