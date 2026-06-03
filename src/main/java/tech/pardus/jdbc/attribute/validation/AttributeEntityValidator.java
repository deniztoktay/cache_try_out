package tech.pardus.jdbc.attribute.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.attribute.entity.AttributeEntity;
import tech.pardus.jdbc.attribute.view.AttributeViewService;
import tech.pardus.jdbc.validation.JdbcEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;

@Component
public class AttributeEntityValidator implements JdbcEntityValidator<AttributeEntity> {

  private static final int NAME_MAX = 100;
  private static final int DESCRIPTION_MAX = 255;
  private static final int TYPE_MAX = 20;
  private static final int OPERANT_USER_MAX = 50;

  private final AttributeViewService attributeViewService;

  public AttributeEntityValidator(AttributeViewService attributeViewService) {
    this.attributeViewService = attributeViewService;
  }

  @Override
  public Mono<Void> validateForFields(AttributeEntity entity) {
    return Mono.fromRunnable(() -> validateFields(entity));
  }

  @Override
  public Mono<Void> validateUq(AttributeEntity entity) {
    return Mono.fromRunnable(
        () -> {
          if (attributeViewService.existsByName(entity.getName(), entity.getId())) {
            throw JdbcValidationException.badRequest(
                "ATTRIBUTE_UK_NAME",
                "Attribute with name '%s' already exists (uk_attribute)".formatted(entity.getName()));
          }
        });
  }

  @Override
  public Mono<Void> validateFk(AttributeEntity entity) {
    return Mono.fromRunnable(
        () -> {
          if (entity.getId() == null) {
            return;
          }
          if (attributeViewService.findById(entity.getId()).isEmpty()) {
            throw JdbcValidationException.badRequest(
                "ATTRIBUTE_NOT_FOUND",
                "Attribute with id %s does not exist".formatted(entity.getId()));
          }
        });
  }

  private static void validateFields(AttributeEntity entity) {
    if (entity == null) {
      throw JdbcValidationException.badRequest("ATTRIBUTE_REQUIRED", "Attribute entity must not be null");
    }
    if (StringUtils.isBlank(entity.getName())) {
      throw JdbcValidationException.badRequest("ATTRIBUTE_NAME_REQUIRED", "Name is required");
    }
    if (entity.getName().length() > NAME_MAX) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_NAME_TOO_LONG", "Name must be at most %d characters".formatted(NAME_MAX));
    }
    if (entity.getDescription() != null && entity.getDescription().length() > DESCRIPTION_MAX) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_DESCRIPTION_TOO_LONG",
          "Description must be at most %d characters".formatted(DESCRIPTION_MAX));
    }
    if (StringUtils.isBlank(entity.getType())) {
      throw JdbcValidationException.badRequest("ATTRIBUTE_TYPE_REQUIRED", "Type is required");
    }
    if (entity.getType().length() > TYPE_MAX) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_TYPE_TOO_LONG", "Type must be at most %d characters".formatted(TYPE_MAX));
    }
    if (entity.getOperantUser() != null && entity.getOperantUser().length() > OPERANT_USER_MAX) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_OPERANT_USER_TOO_LONG",
          "OperantUser must be at most %d characters".formatted(OPERANT_USER_MAX));
    }
    if (entity.getId() != null && entity.getId() <= 0) {
      throw JdbcValidationException.badRequest(
          "ATTRIBUTE_ID_INVALID", "AttributeId must be positive when set");
    }
  }

  public static void applyInsertDefaults(AttributeEntity entity) {
    if (entity.getIsGmp() == null) {
      entity.setIsGmp(Boolean.FALSE);
    }
    if (entity.getShowToAdmin() == null) {
      entity.setShowToAdmin(Boolean.TRUE);
    }
    if (entity.getShowToUser() == null) {
      entity.setShowToUser(Boolean.TRUE);
    }
  }
}
