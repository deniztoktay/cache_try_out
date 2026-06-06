package tech.pardus.newdesign.redis.codec;

/** Raised when a cache payload cannot be serialized or deserialized. */
public class CacheSerializationException extends RuntimeException {

  private static final long serialVersionUID = 6463667715674981954L;

  public CacheSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
