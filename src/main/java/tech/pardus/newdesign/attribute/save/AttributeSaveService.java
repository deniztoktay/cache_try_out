package tech.pardus.newdesign.attribute.save;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.pardus.attribute.mapper.AttributeMapper;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.jdbc.AfterCommitCallback;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.attribute.validation.AttributeEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;
import tech.pardus.newdesign.attribute.jdbc.entity.AttributeEntity;
import tech.pardus.newdesign.attribute.jdbc.repository.AttributesJpaRepository;
import tech.pardus.newdesign.write.SingleValueCacheWriteSync;

@Service
public class AttributeSaveService {

  private final TransactionalSaveOrchestrator orchestrator;
  private final AttributeEntityValidator validator;
  private final AttributesJpaRepository repository;
  private final AttributeMapper attributeMapper;
  private final SingleValueCacheWriteSync<Integer, AttributeModel> cacheWriteSync;

  public AttributeSaveService(
      TransactionalSaveOrchestrator orchestrator,
      AttributeEntityValidator validator,
      AttributesJpaRepository repository,
      AttributeMapper attributeMapper,
      @Qualifier("newDesignAttributeCacheWriteSync")
          SingleValueCacheWriteSync<Integer, AttributeModel> cacheWriteSync) {
    this.orchestrator = orchestrator;
    this.validator = validator;
    this.repository = repository;
    this.attributeMapper = attributeMapper;
    this.cacheWriteSync = cacheWriteSync;
  }

  public Mono<AttributeEntity> create(AttributeEntity entity) {
    AttributeEntityValidator.applyInsertDefaults(entity);
    return orchestrator.executeInsert(
        entity, validator, repository::save, List.of(afterInsert(entity)));
  }

  public Mono<AttributeEntity> update(AttributeEntity entity) {
    return orchestrator.executeUpdate(
        entity, validator, repository::save, List.of(afterUpdate(entity)));
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
              var context =
                  new SingleValueCacheWriteSync.DeleteContext<>(attributeMapper.toModel(existing));
              return orchestrator.executeTransactionalDelete(
                  List.of(() -> validator.validateFk(stub).block()),
                  List.of(afterDelete(id, context)),
                  () -> repository.deleteById(id));
            });
  }

  private AfterCommitCallback afterInsert(AttributeEntity entity) {
    return () -> cacheWriteSync.afterInsert(attributeMapper.toModel(entity)).subscribe();
  }

  private AfterCommitCallback afterUpdate(AttributeEntity entity) {
    return () -> cacheWriteSync.afterUpdate(attributeMapper.toModel(entity)).subscribe();
  }

  private AfterCommitCallback afterDelete(
      Integer id, SingleValueCacheWriteSync.DeleteContext<AttributeModel> context) {
    return () -> cacheWriteSync.afterDelete(id, context).subscribe();
  }
}
