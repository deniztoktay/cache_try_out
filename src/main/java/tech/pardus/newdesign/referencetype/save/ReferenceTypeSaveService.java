package tech.pardus.newdesign.referencetype.save;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.AfterCommitCallback;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.referencetype.validation.ReferenceTypeEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;
import tech.pardus.newdesign.referencetype.jdbc.entity.ReferenceTypeEntity;
import tech.pardus.newdesign.referencetype.jdbc.repository.ReferenceTypeJpaRepository;
import tech.pardus.newdesign.write.SingleValueCacheWriteSync;
import tech.pardus.referencetype.mapper.ReferenceTypeMapper;
import tech.pardus.referencetype.model.ReferenceTypeModel;

@Service
public class ReferenceTypeSaveService {

  private final TransactionalSaveOrchestrator orchestrator;
  private final ReferenceTypeEntityValidator validator;
  private final ReferenceTypeJpaRepository repository;
  private final ReferenceTypeMapper mapper;
  private final SingleValueCacheWriteSync<Integer, ReferenceTypeModel> cacheWriteSync;

  public ReferenceTypeSaveService(
      TransactionalSaveOrchestrator orchestrator,
      ReferenceTypeEntityValidator validator,
      ReferenceTypeJpaRepository repository,
      ReferenceTypeMapper mapper,
      @Qualifier("newDesignReferenceTypeCacheWriteSync")
          SingleValueCacheWriteSync<Integer, ReferenceTypeModel> cacheWriteSync) {
    this.orchestrator = orchestrator;
    this.validator = validator;
    this.repository = repository;
    this.mapper = mapper;
    this.cacheWriteSync = cacheWriteSync;
  }

  public Mono<ReferenceTypeEntity> create(ReferenceTypeEntity entity) {
    ReferenceTypeEntityValidator.applyInsertDefaults(entity);
    return orchestrator.executeInsert(
        entity, validator, repository::save, List.of(afterInsert(entity)));
  }

  public Mono<ReferenceTypeEntity> update(ReferenceTypeEntity entity) {
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
                                    "REFERENCE_TYPE_NOT_FOUND",
                                    "ReferenceType with id %s does not exist".formatted(id)))))
        .flatMap(
            existing -> {
              var stub = ReferenceTypeEntity.builder().id(id).build();
              var context =
                  new SingleValueCacheWriteSync.DeleteContext<>(mapper.toModel(existing));
              return orchestrator.executeTransactionalDelete(
                  List.of(() -> validator.validateFk(stub).block()),
                  List.of(afterDelete(id, context)),
                  () -> repository.deleteById(id));
            });
  }

  private AfterCommitCallback afterInsert(ReferenceTypeEntity entity) {
    return () -> cacheWriteSync.afterInsert(mapper.toModel(entity)).subscribe();
  }

  private AfterCommitCallback afterUpdate(ReferenceTypeEntity entity) {
    return () -> cacheWriteSync.afterUpdate(mapper.toModel(entity)).subscribe();
  }

  private AfterCommitCallback afterDelete(
      Integer id, SingleValueCacheWriteSync.DeleteContext<ReferenceTypeModel> context) {
    return () -> cacheWriteSync.afterDelete(id, context).subscribe();
  }
}
