package tech.pardus.exception;

public class L2WaitTimeoutException extends RuntimeException {

  private static final long serialVersionUID = 1721016762614919828L;

  public L2WaitTimeoutException(String message) {
    super(message);
  }
}
