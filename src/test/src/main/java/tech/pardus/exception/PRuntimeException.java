package tech.pardus.exception;

import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;


@Getter
@Setter
@Builder
public class PRuntimeException extends RuntimeException {
  private static final long serialVersionUID = -5280660962757528343L;
  private final PProblem mlmTrackingProblem;

  public PRuntimeException(PProblem mlmTrackingProblem) {
    super(mlmTrackingProblem.getDetail());
    this.mlmTrackingProblem = mlmTrackingProblem;
  }

  public PRuntimeException(PProblem mlmTrackingProblem, Throwable cause) {
    super(mlmTrackingProblem.getDetail(), cause);
    this.mlmTrackingProblem = mlmTrackingProblem;
  }

  public static class PRuntimeExceptionBuilder {
    private String condition;
    private int status;
    private String type;
    private String title;
    private String detail;
    private boolean showBackendMessage;
    private Throwable cause;

    public PRuntimeExceptionBuilder condition(String condition) {
      this.condition = condition;
      return this;
    }

    public PRuntimeExceptionBuilder status(int status) {
      this.status = status;
      return this;
    }

    public PRuntimeExceptionBuilder status(HttpStatus status) {
      this.status = status.value();
      return this;
    }

    public PRuntimeExceptionBuilder type(String val) {
      this.type = val;
      return this;
    }

    public PRuntimeExceptionBuilder title(String val) {
      this.title = val;
      return this;
    }

    public PRuntimeExceptionBuilder detail(String val) {
      this.detail = val;
      return this;
    }

    public PRuntimeExceptionBuilder show() {
      this.showBackendMessage = true;
      return this;
    }

    public PRuntimeExceptionBuilder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public PRuntimeException build() {
      var problem =
          PProblem.builder()
              .condition(condition)
              .status(status)
              .type(type)
              .title(title)
              .detail(detail)
              .showBackendMessage(showBackendMessage)
              .build();
      return Objects.isNull(this.cause)
          ? new PRuntimeException(problem)
          : new PRuntimeException(problem, cause);
    }
  }
}
