package tech.pardus.newdesign.attributetag.save;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.AfterCommitCallback;
import tech.pardus.jdbc.TransactionalSaveOrchestrator;
import tech.pardus.jdbc.attributetag.validation.AttributeTagEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;
import tech.pardus.newdesign.attributetag.jdbc.entity.AttributeTagEntity;
import tech.pardus.newdesign.attributetag.jdbc.entity.AttributeTagId;
import tech.pardus.newdesign.attributetag.jdbc.repository.AttributeTagJpaRepository;
import tech.pardus.newdesign.write.GroupedIdListCacheWriteSync;

@Service
public class AttributeTagSaveService {

  private final TransactionalSaveOrchestrator orchestrator;
  private final AttributeTagEntityValidator validator;
  private final AttributeTagJpaRepository repository;
  private final GroupedIdListCacheWriteSync cacheWriteSync;

  public AttributeTagSaveService(
      TransactionalSaveOrchestrator orchestrator,
      AttributeTagEntityValidator validator,
      AttributeTagJpaRepository repository,
      @Qualifier("newDesignAttributeTagCacheWriteSync")
          GroupedIdListCacheWriteSync cacheWriteSync) {
    this.orchestrator = orchestrator;
    this.validator = validator;
    this.repository = repository;
    this.cacheWriteSync = cacheWriteSync;
  }

  public Mono<AttributeTagEntity> create(AttributeTagEntity entity) {
    return orchestrator.executeInsert(
        entity, validator, repository::save, List.of(afterInsert(entity)));
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
                                JdbcValidationException.badRequest(
                                    "ATTRIBUTE_TAG_NOT_FOUND",
                                    "AttributeTag not found for attributeId=%s tagId=%s"
                                        .formatted(attributeId, tagId)))))
        .flatMap(
            existing ->
                orchestrator.executeTransactionalDelete(
                    List.of(() -> validator.validateLinkExists(attributeId, tagId).block()),
                    List.of(afterDelete(attributeId, tagId)),
                    () -> repository.deleteById(linkId)));
  }

  private AfterCommitCallback afterInsert(AttributeTagEntity entity) {
    return () ->
        cacheWriteSync
            .afterLinkInsert(entity.getAttributeId(), entity.getTagId())
            .subscribe();
  }

  private AfterCommitCallback afterDelete(Integer attributeId, Integer tagId) {
    return () -> cacheWriteSync.afterLinkDelete(attributeId, tagId).subscribe();
  }
}
