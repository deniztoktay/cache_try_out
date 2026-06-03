package tech.pardus.jdbc.format.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.format.entity.FormatTypeEntity;
import tech.pardus.jdbc.format.view.FormatTypeViewService;
import tech.pardus.jdbc.validation.JdbcEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;

@Component
public class FormatTypeEntityValidator implements JdbcEntityValidator<FormatTypeEntity> {

  private static final int FORMAT_VALUE_MAX = 100;
  private static final int DESCRIPTION_MAX = 255;
  private static final int TYPE_MAX = 50;
  private static final int CULTURE_MAX = 25;

  private final FormatTypeViewService formatTypeViewService;

  public FormatTypeEntityValidator(FormatTypeViewService formatTypeViewService) {
    this.formatTypeViewService = formatTypeViewService;
  }

  @Override
  public Mono<Void> validateForFields(FormatTypeEntity entity) {
    return Mono.fromRunnable(() -> validateFields(entity));
  }

  @Override
  public Mono<Void> validateUq(FormatTypeEntity entity) {
    return Mono.fromRunnable(
        () -> {
          if (formatTypeViewService.existsByFormatValueAndCulture(
              entity.getFormatValue(), entity.getCulture(), entity.getId())) {
            throw JdbcValidationException.badRequest(
                "FORMAT_TYPE_UK_VALUE_CULTURE",
                "FormatType with value '%s' and culture '%s' already exists (Uk_FormatType)"
                    .formatted(entity.getFormatValue(), entity.getCulture()));
          }
        });
  }

  @Override
  public Mono<Void> validateFk(FormatTypeEntity entity) {
    return Mono.fromRunnable(
        () -> {
          if (entity.getId() == null) {
            return;
          }
          if (formatTypeViewService.findById(entity.getId()).isEmpty()) {
            throw JdbcValidationException.badRequest(
                "FORMAT_TYPE_NOT_FOUND",
                "FormatType with id %s does not exist".formatted(entity.getId()));
          }
        });
  }

  private static void validateFields(FormatTypeEntity entity) {
    if (entity == null) {
      throw JdbcValidationException.badRequest(
          "FORMAT_TYPE_REQUIRED", "FormatType entity must not be null");
    }
    if (StringUtils.isBlank(entity.getFormatValue())) {
      throw JdbcValidationException.badRequest(
          "FORMAT_TYPE_VALUE_REQUIRED", "FormatValue is required");
    }
    if (entity.getFormatValue().length() > FORMAT_VALUE_MAX) {
      throw JdbcValidationException.badRequest(
          "FORMAT_TYPE_VALUE_TOO_LONG",
          "FormatValue must be at most %d characters".formatted(FORMAT_VALUE_MAX));
    }
    if (entity.getDescription() != null && entity.getDescription().length() > DESCRIPTION_MAX) {
      throw JdbcValidationException.badRequest(
          "FORMAT_TYPE_DESCRIPTION_TOO_LONG",
          "Description must be at most %d characters".formatted(DESCRIPTION_MAX));
    }
    if (entity.getType() != null && entity.getType().length() > TYPE_MAX) {
      throw JdbcValidationException.badRequest(
          "FORMAT_TYPE_TYPE_TOO_LONG", "Type must be at most %d characters".formatted(TYPE_MAX));
    }
    if (entity.getCulture() != null && entity.getCulture().length() > CULTURE_MAX) {
      throw JdbcValidationException.badRequest(
          "FORMAT_TYPE_CULTURE_TOO_LONG",
          "Culture must be at most %d characters".formatted(CULTURE_MAX));
    }
    if (entity.getId() != null && entity.getId() <= 0) {
      throw JdbcValidationException.badRequest(
          "FORMAT_TYPE_ID_INVALID", "FormatTypeId must be positive when set");
    }
  }
}
