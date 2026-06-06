package tech.pardus.newdesign.tag.save;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.AfterCommitCallback;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.tag.validation.TagEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;
import tech.pardus.newdesign.tag.jdbc.entity.TagEntity;
import tech.pardus.newdesign.tag.jdbc.repository.TagsJpaRepository;
import tech.pardus.newdesign.tag.model.TagUsageType;
import tech.pardus.newdesign.write.SingleValueCacheWriteSync;
import tech.pardus.tag.model.TagModel;

@Service
public class TagSaveService {

  private final TransactionalSaveOrchestrator orchestrator;
  private final TagEntityValidator validator;
  private final TagsJpaRepository repository;
  private final SingleValueCacheWriteSync<Integer, TagModel> cacheWriteSync;

  public TagSaveService(
      TransactionalSaveOrchestrator orchestrator,
      TagEntityValidator validator,
      TagsJpaRepository repository,
      @Qualifier("newDesignTagCacheWriteSync") SingleValueCacheWriteSync<Integer, TagModel> cacheWriteSync) {
    this.orchestrator = orchestrator;
    this.validator = validator;
    this.repository = repository;
    this.cacheWriteSync = cacheWriteSync;
  }

  public Mono<TagEntity> create(
      String name, String type, TagUsageType usageType, Boolean canUserAssign) {
    var entity =
        TagEntity.builder().name(name).type(type).usageType(usageType).canUserAssign(canUserAssign).build();
    TagEntityValidator.applyInsertDefaults(entity);
    return orchestrator.executeInsert(
        entity, validator, repository::save, List.of(afterInsert(entity)));
  }

  public Mono<TagEntity> update(TagEntity entity) {
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
                                    "TAG_NOT_FOUND",
                                    "Tag with id %s does not exist".formatted(id)))))
        .flatMap(
            existing -> {
              var stub = TagEntity.builder().id(id).build();
              var context = new SingleValueCacheWriteSync.DeleteContext<>(toModel(existing));
              return orchestrator.executeTransactionalDelete(
                  List.of(() -> validator.validateFk(stub).block()),
                  List.of(afterDelete(id, context)),
                  () -> repository.deleteById(id));
            });
  }

  private AfterCommitCallback afterInsert(TagEntity entity) {
    return () -> cacheWriteSync.afterInsert(toModel(entity)).subscribe();
  }

  private AfterCommitCallback afterUpdate(TagEntity entity) {
    return () -> cacheWriteSync.afterUpdate(toModel(entity)).subscribe();
  }

  private AfterCommitCallback afterDelete(
      Integer id, SingleValueCacheWriteSync.DeleteContext<TagModel> context) {
    return () -> cacheWriteSync.afterDelete(id, context).subscribe();
  }

  private static TagModel toModel(TagEntity entity) {
    return new TagModel(
        entity.getId(),
        entity.getName(),
        entity.getType(),
        entity.getUsageType() != null ? entity.getUsageType().name() : null,
        entity.getCanUserAssign());
  }
}
