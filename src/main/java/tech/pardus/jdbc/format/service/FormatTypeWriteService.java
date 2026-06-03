package tech.pardus.jdbc.format.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.pardus.cache.write.AbstractCachedSaveService;
import tech.pardus.cache.write.CacheWriteSync;
import tech.pardus.format.mapper.FormatTypeMapper;
import tech.pardus.format.model.FormatTypeModel;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.format.entity.FormatTypeEntity;
import tech.pardus.jdbc.format.repository.FormatTypeJpaRepository;
import tech.pardus.jdbc.format.validation.FormatTypeEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;

@Service
public class FormatTypeWriteService
    extends AbstractCachedSaveService<FormatTypeEntity, Integer, FormatTypeModel> {

  private final FormatTypeEntityValidator validator;
  private final FormatTypeJpaRepository repository;
  private final FormatTypeMapper mapper;

  public FormatTypeWriteService(
      TransactionalSaveOrchestrator orchestrator,
      FormatTypeEntityValidator validator,
      FormatTypeJpaRepository repository,
      FormatTypeMapper mapper,
      @Qualifier("formatTypeCacheWriteSync")
          CacheWriteSync<Integer, FormatTypeModel> formatTypeCacheWriteSync) {
    super(orchestrator, formatTypeCacheWriteSync, mapper::toModel);
    this.validator = validator;
    this.repository = repository;
    this.mapper = mapper;
  }

  public Mono<FormatTypeEntity> create(FormatTypeEntity entity) {
    return insertCached(entity, validator, repository::save);
  }

  public Mono<FormatTypeEntity> update(FormatTypeEntity entity) {
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
                                    "FORMAT_TYPE_NOT_FOUND",
                                    "FormatType with id %s does not exist".formatted(id)))))
        .flatMap(
            existing -> {
              var model = mapper.toModel(existing);
              var alias = model.valueCultureAliasStringId();
              var context = new CacheWriteSync.DeleteContext<>(model, alias);
              return delete(
                  List.of(() -> validator.validateFk(existing).block()),
                  List.of(afterDelete(id, context)),
                  () -> repository.deleteById(id));
            });
  }
}
