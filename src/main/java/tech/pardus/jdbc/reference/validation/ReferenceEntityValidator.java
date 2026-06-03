package tech.pardus.jdbc.reference.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.jdbc.reference.entity.ReferenceEntity;
import tech.pardus.jdbc.reference.view.ReferenceViewService;
import tech.pardus.jdbc.validation.JdbcEntityValidator;
import tech.pardus.jdbc.validation.JdbcValidationException;

@Component
public class ReferenceEntityValidator implements JdbcEntityValidator<ReferenceEntity> {

  private static final int VALUE_MAX = 255;
  private static final int USER_ID_MAX = 50;

  private final ReferenceViewService referenceViewService;

  public ReferenceEntityValidator(ReferenceViewService referenceViewService) {
    this.referenceViewService = referenceViewService;
  }

  @Override
  public Mono<Void> validateForFields(ReferenceEntity entity) {
    return Mono.fromRunnable(() -> validateFields(entity));
  }

  @Override
  public Mono<Void> validateUq(ReferenceEntity entity) {
    return Mono.fromRunnable(
        () -> {
          if (referenceViewService.existsByReferenceTypeIdAndValue(
              entity.getReferenceTypeId(), entity.getValue(), entity.getId())) {
            throw JdbcValidationException.badRequest(
                "REFERENCE_UK_TYPE_VALUE",
                "Reference with type %s and value '%s' already exists (Uk_reference)"
                    .formatted(entity.getReferenceTypeId(), entity.getValue()));
          }
        });
  }

  @Override
  public Mono<Void> validateFk(ReferenceEntity entity) {
    return Mono.fromRunnable(
        () -> {
          if (entity.getId() == null) {
            return;
          }
          if (referenceViewService.findById(entity.getId()).isEmpty()) {
            throw JdbcValidationException.badRequest(
                "REFERENCE_NOT_FOUND",
                "Reference with id %s does not exist".formatted(entity.getId()));
          }
        });
  }

  private static void validateFields(ReferenceEntity entity) {
    if (entity == null) {
      throw JdbcValidationException.badRequest(
          "REFERENCE_REQUIRED", "Reference entity must not be null");
    }
    if (StringUtils.isBlank(entity.getValue())) {
      throw JdbcValidationException.badRequest(
          "REFERENCE_VALUE_REQUIRED", "Value is required");
    }
    if (entity.getValue().length() > VALUE_MAX) {
      throw JdbcValidationException.badRequest(
          "REFERENCE_VALUE_TOO_LONG",
          "Value must be at most %d characters".formatted(VALUE_MAX));
    }
    if (StringUtils.isBlank(entity.getUserId())) {
      throw JdbcValidationException.badRequest(
          "REFERENCE_USER_ID_REQUIRED", "UserId is required");
    }
    if (entity.getUserId().length() > USER_ID_MAX) {
      throw JdbcValidationException.badRequest(
          "REFERENCE_USER_ID_TOO_LONG",
          "UserId must be at most %d characters".formatted(USER_ID_MAX));
    }
    if (entity.getId() != null && entity.getId() <= 0) {
      throw JdbcValidationException.badRequest(
          "REFERENCE_ID_INVALID", "ReferenceId must be positive when set");
    }
  }
}
