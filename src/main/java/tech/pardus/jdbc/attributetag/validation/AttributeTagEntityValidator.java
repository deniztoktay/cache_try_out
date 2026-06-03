package tech.pardus.jdbc.attributetag.validation;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.attribute.entity.AttributeTagEntity;
import tech.pardus.jdbc.attribute.view.AttributeViewService;
import tech.pardus.jdbc.attributetag.view.AttributeTagViewService;
import tech.pardus.jdbc.tag.view.TagViewService;
import tech.pardus.jdbc.validation.JdbcEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;

@Component
public class AttributeTagEntityValidator implements JdbcEntityValidator<AttributeTagEntity> {

  private final AttributeTagViewService attributeTagViewService;
  private final AttributeViewService attributeViewService;
  private final TagViewService tagViewService;

  public AttributeTagEntityValidator(
      AttributeTagViewService attributeTagViewService,
      AttributeViewService attributeViewService,
      TagViewService tagViewService) {
    this.attributeTagViewService = attributeTagViewService;
    this.attributeViewService = attributeViewService;
    this.tagViewService = tagViewService;
  }

  @Override
  public Mono<Void> validateForFields(AttributeTagEntity entity) {
    return Mono.fromRunnable(() -> validateFields(entity));
  }

  @Override
  public Mono<Void> validateUq(AttributeTagEntity entity) {
    return Mono.fromRunnable(
        () -> {
          if (attributeTagViewService.existsByAttributeIdAndTagId(
              entity.getAttributeId(), entity.getTagId())) {
            throw JdbcValidationException.badRequest(
                "ATTRIBUTE_TAG_EXISTS",
                "AttributeTag already exists for attributeId=%s tagId=%s"
                    .formatted(entity.getAttributeId(), entity.getTagId()));
          }
        });
  }

  @Override
  public Mono<Void> validateFk(AttributeTagEntity entity) {
    return Mono.fromRunnable(() -> validateParentEntitiesExist(entity));
  }

  /** Validates link row exists (for delete / update). */
  public Mono<Void> validateLinkExists(Integer attributeId, Integer tagId) {
    return Mono.fromRunnable(
        () -> {
          if (attributeTagViewService.findByKey(attributeId, tagId).isEmpty()) {
            throw JdbcValidationException.badRequest(
                "ATTRIBUTE_TAG_NOT_FOUND",
                "AttributeTag not found for attributeId=%s tagId=%s"
                    .formatted(attributeId, tagId));
          }
        });
  }

  private void validateParentEntitiesExist(AttributeTagEntity entity) {
    if (attributeViewService.findById(entity.getAttributeId()).isEmpty()) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_NOT_FOUND",
          "Attribute with id %s does not exist".formatted(entity.getAttributeId()));
    }
    if (tagViewService.findById(entity.getTagId()).isEmpty()) {
      throw JdbcValidationException.badRequest(
          "TAG_NOT_FOUND", "Tag with id %s does not exist".formatted(entity.getTagId()));
    }
  }

  private static void validateFields(AttributeTagEntity entity) {
    if (entity == null) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_TAG_REQUIRED", "AttributeTag entity must not be null");
    }
    if (entity.getAttributeId() == null) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_TAG_ATTRIBUTE_REQUIRED", "AttributeId is required");
    }
    if (entity.getTagId() == null) {
      throw JdbcValidationException.badRequest("ATTRIBUTE_TAG_TAG_REQUIRED", "TagId is required");
    }
    if (entity.getAttributeId() <= 0 || entity.getTagId() <= 0) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_TAG_ID_INVALID", "AttributeId and TagId must be positive");
    }
    if (entity.getUserId() != null && entity.getUserId().length() > 50) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_TAG_USER_ID_TOO_LONG", "UserId must be at most 50 characters");
    }
  }
}
