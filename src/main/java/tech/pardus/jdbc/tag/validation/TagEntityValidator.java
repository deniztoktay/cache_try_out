package tech.pardus.jdbc.tag.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.tag.entity.TagEntity;
import tech.pardus.jdbc.tag.enums.TagUsageType;
import tech.pardus.jdbc.tag.view.TagViewService;
import tech.pardus.jdbc.validation.JdbcEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;

@Component
public class TagEntityValidator implements JdbcEntityValidator<TagEntity> {
  private static final int NAME_MAX = 50;
  private static final int TYPE_MAX = 25;

  private final TagViewService tagViewService;

  public TagEntityValidator(TagViewService tagViewService) {
    this.tagViewService = tagViewService;
  }

  @Override
  public Mono<Void> validateForFields(TagEntity entity) {
    return Mono.fromRunnable(() -> validateFields(entity));
  }

  @Override
  public Mono<Void> validateUq(TagEntity entity) {
    return Mono.fromRunnable(
        () -> {
          if (tagViewService.existsByNameAndType(entity.getName(), entity.getType(), entity.getId())) {
            throw JdbcValidationException.badRequest(
                "TAG_UK_NAME_TYPE",
                "Tag with name '%s' and type '%s' already exists (Uk_Tags)".formatted(
                    entity.getName(), entity.getType()));
          }
        });
  }

  @Override
  public Mono<Void> validateFk(TagEntity entity) {
    return Mono.fromRunnable(
        () -> {
          if (entity.getId() == null) {
            return;
          }
          if (tagViewService.findById(entity.getId()).isEmpty()) {
            throw JdbcValidationException.badRequest(
                "TAG_NOT_FOUND", "Tag with id %s does not exist".formatted(entity.getId()));
          }
        });
  }

  private static void validateFields(TagEntity entity) {
    if (entity == null) {
      throw JdbcValidationException.badRequest("TAG_REQUIRED", "Tag entity must not be null");
    }
    if (StringUtils.isBlank(entity.getName())) {
      throw JdbcValidationException.badRequest("TAG_NAME_REQUIRED", "Name is required");
    }
    if (entity.getName().length() > NAME_MAX) {
      throw JdbcValidationException.badRequest(
          "TAG_NAME_TOO_LONG", "Name must be at most %d characters".formatted(NAME_MAX));
    }
    if (StringUtils.isBlank(entity.getType())) {
      throw JdbcValidationException.badRequest("TAG_TYPE_REQUIRED", "Type is required");
    }
    if (entity.getType().length() > TYPE_MAX) {
      throw JdbcValidationException.badRequest(
          "TAG_TYPE_TOO_LONG", "Type must be at most %d characters".formatted(TYPE_MAX));
    }
    if (entity.getUsageType() != null && entity.getUsageType().name().length() > 6) {
      throw JdbcValidationException.badRequest(
          "TAG_USAGE_TYPE_INVALID", "UsageType must fit varchar(6)");
    }
    if (entity.getId() != null && entity.getId() <= 0) {
      throw JdbcValidationException.badRequest("TAG_ID_INVALID", "TagId must be positive when set");
    }
  }

  /** Applies DB defaults before insert when fields are omitted. */
  public static void applyInsertDefaults(TagEntity entity) {
    if (entity.getUsageType() == null) {
      entity.setUsageType(TagUsageType.SYSTEM);
    }
    if (entity.getCanUserAssign() == null) {
      entity.setCanUserAssign(Boolean.FALSE);
    }
  }
}
