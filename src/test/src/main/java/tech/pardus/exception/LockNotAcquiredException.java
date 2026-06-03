package tech.pardus.exception;

public class LockNotAcquiredException extends RuntimeException {

  private static final long serialVersionUID = -1560684891362255169L;

  public LockNotAcquiredException(String message) {
    super(message);
  }
}
