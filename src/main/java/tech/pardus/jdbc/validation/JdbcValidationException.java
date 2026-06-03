package tech.pardus.jdbc.validation;

import org.springframework.http.HttpStatus;
import tech.pardus.exception.PRuntimeException;

public final class JdbcValidationException {

  private JdbcValidationException() {}

  public static PRuntimeException badRequest(String condition, String detail) {
    return PRuntimeException.builder()
        .condition(condition)
        .status(HttpStatus.BAD_REQUEST)
        .type("VALIDATION_FAILED")
        .title("Validation failed")
        .detail(detail)
        .build();
  }
}
