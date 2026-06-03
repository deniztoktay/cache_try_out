package tech.pardus.exception;

import org.springframework.http.HttpStatus;

public class ErrorWrapper {

  public static PRuntimeException wrapInternalError(
      String condition, String type, String title, String detail, Throwable e) {
    return PRuntimeException.builder()
        .condition(condition)
        .type(type)
        .title(title)
        .detail(detail)
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .cause(e)
        .build();
  }

  public static PRuntimeException wrapInternalError(
      String condition, String type, String title, String detail, Throwable e, HttpStatus status) {
    return PRuntimeException.builder()
        .condition(condition)
        .type(type)
        .title(title)
        .detail(detail)
        .status(status)
        .cause(e)
        .build();
  }

  public static PRuntimeException wrapError(
      String condition, String type, String title, String detail, HttpStatus status, Throwable e) {
    return PRuntimeException.builder()
        .condition(condition)
        .type(type)
        .title(title)
        .detail(detail)
        .status(status)
        .cause(e)
        .show()
        .build();
  }

  public static PRuntimeException wrapError(
      String condition, String type, String title, String detail, HttpStatus status) {
    return PRuntimeException.builder()
        .condition(condition)
        .type(type)
        .title(title)
        .detail(detail)
        .status(status)
        .show()
        .build();
  }
}
