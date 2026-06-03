package tech.pardus.exception;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PProblem implements Serializable {
  private static final long serialVersionUID = -8684715019493582373L;
  private String condition;
  private int status;
  private String type;
  private String title;
  private String detail;
  @Builder.Default private boolean showBackendMessage = false;
}
