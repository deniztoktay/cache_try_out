package tech.pardus.jdbc.reference.service;

import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.pardus.cache.write.AbstractJdbcSaveService;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.reference.entity.ReferenceEntity;
import tech.pardus.jdbc.reference.repository.ReferenceJpaRepository;
import tech.pardus.jdbc.reference.validation.ReferenceEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;

@Service
public class ReferenceWriteService extends AbstractJdbcSaveService<ReferenceEntity> {

  private final ReferenceEntityValidator validator;
  private final ReferenceJpaRepository repository;

  public ReferenceWriteService(
      TransactionalSaveOrchestrator orchestrator,
      ReferenceEntityValidator validator,
      ReferenceJpaRepository repository) {
    super(orchestrator);
    this.validator = validator;
    this.repository = repository;
  }

  public Mono<ReferenceEntity> create(ReferenceEntity entity) {
    return insert(entity, validator, repository::save, List.of());
  }

  public Mono<ReferenceEntity> update(ReferenceEntity entity) {
    return update(entity, validator, repository::save, List.of());
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
                                    "REFERENCE_NOT_FOUND",
                                    "Reference with id %s does not exist".formatted(id)))))
        .flatMap(
            existing -> {
              var stub = new ReferenceEntity();
              stub.setId(id);
              return delete(
                  List.of(() -> validator.validateFk(stub).block()),
                  List.of(),
                  () -> repository.deleteById(id));
            });
  }
}
