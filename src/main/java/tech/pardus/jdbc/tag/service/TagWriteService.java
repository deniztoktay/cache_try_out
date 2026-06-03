package tech.pardus.jdbc.tag.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.pardus.cache.write.AbstractCachedSaveService;
import tech.pardus.cache.write.CacheWriteSync;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.tag.entity.TagEntity;
import tech.pardus.jdbc.tag.enums.TagUsageType;
import tech.pardus.jdbc.tag.repository.TagsJpaRepository;
import tech.pardus.jdbc.tag.validation.TagEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;
import tech.pardus.tag.model.TagModel;

@Service
public class TagWriteService extends AbstractCachedSaveService<TagEntity, Integer, TagModel> {

  private final TagEntityValidator validator;
  private final TagsJpaRepository repository;

  public TagWriteService(
      TransactionalSaveOrchestrator orchestrator,
      TagEntityValidator validator,
      TagsJpaRepository repository,
      @Qualifier("tagCacheWriteSync") CacheWriteSync<Integer, TagModel> tagCacheWriteSync) {
    super(orchestrator, tagCacheWriteSync, TagWriteService::toModel);
    this.validator = validator;
    this.repository = repository;
  }

  public Mono<TagEntity> create(String name, String type, TagUsageType usageType, Boolean canUserAssign) {
    var entity =
        TagEntity.builder().name(name).type(type).usageType(usageType).canUserAssign(canUserAssign).build();
    TagEntityValidator.applyInsertDefaults(entity);
    return insertCached(entity, validator, repository::save);
  }

  public Mono<TagEntity> update(TagEntity entity) {
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
                                    "TAG_NOT_FOUND", "Tag with id %s does not exist".formatted(id)))))
        .flatMap(
            existing -> {
              var stub = TagEntity.builder().id(id).build();
              var context = deleteContext(existing);
              return delete(
                  List.of(() -> validator.validateFk(stub).block()),
                  List.of(afterDelete(id, context)),
                  () -> repository.deleteById(id));
            });
  }

  private static CacheWriteSync.DeleteContext<TagModel> deleteContext(TagEntity existing) {
    var model = toModel(existing);
    var alias =
        model.name() != null && !model.name().isBlank() ? model.nameAliasStringId() : null;
    return new CacheWriteSync.DeleteContext<>(model, alias);
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
