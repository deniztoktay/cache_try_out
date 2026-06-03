package tech.pardus.jdbc.attributetag.view;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tech.pardus.attributetag.cache.AttributeTagEntityLoader;
import tech.pardus.attributetag.model.AttributeTagKeys;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.cache.read.AbstractValidationView;
import tech.pardus.cache.read.CacheReadStrategy;

@Service
public class AttributeTagViewService
    extends AbstractValidationView<String, AttributeTagView, AttributeTagModel> {

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  private final AttributeTagEntityLoader entityLoader;
  private final CacheReadStrategy<String, AttributeTagModel> readStrategy;

  public AttributeTagViewService(
      @Qualifier("attributeTagCacheReadStrategy")
          CacheReadStrategy<String, AttributeTagModel> attributeTagCacheReadStrategy,
      AttributeTagEntityLoader entityLoader) {
    super(attributeTagCacheReadStrategy, AttributeTagView::fromModel, READ_TIMEOUT);
    this.entityLoader = entityLoader;
    this.readStrategy = attributeTagCacheReadStrategy;
  }

  public Optional<AttributeTagView> findByKey(Integer attributeId, Integer tagId) {
    if (attributeId == null || tagId == null) {
      return Optional.empty();
    }
    return findByIdForValidation(AttributeTagKeys.memberId(attributeId, tagId));
  }

  /** Composite PK (AttributeId, TagId). */
  public boolean existsByAttributeIdAndTagId(Integer attributeId, Integer tagId) {
    return findByKey(attributeId, tagId).isPresent();
  }

  /** Whether any link exists for the attribute. */
  public boolean existsForAttribute(Integer attributeId) {
    if (attributeId == null) {
      return false;
    }
    return findAllForValidation().stream()
        .anyMatch(link -> Objects.equals(link.attributeId(), attributeId));
  }

  /** Whether any link exists for the tag. */
  public boolean existsForTag(Integer tagId) {
    if (tagId == null) {
      return false;
    }
    return findAllForValidation().stream().anyMatch(link -> Objects.equals(link.tagId(), tagId));
  }

  /** Tier-aware single-key read (L1 → L2 → DB). */
  public Optional<AttributeTagView> findByKeyFromCache(Integer attributeId, Integer tagId) {
    if (attributeId == null || tagId == null) {
      return Optional.empty();
    }
    var memberId = AttributeTagKeys.memberId(attributeId, tagId);
    return readStrategy
        .getByMemberId(memberId, entityLoader.findById(memberId))
        .map(AttributeTagView::fromModel)
        .blockOptional(READ_TIMEOUT);
  }
}
