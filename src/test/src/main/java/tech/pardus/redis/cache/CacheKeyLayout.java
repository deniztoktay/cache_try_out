package tech.pardus.redis.cache;

/**
 * Redis key naming for live data, temporary initialization buffers, and member indexes.
 *
 * <p>Live: {@code {ns}:idx}, {@code {ns}:v:{memberId}}<br>
 * Temp: {@code {ns}:tmp:{runId}:idx}, {@code {ns}:tmp:{runId}:v:{memberId}}<br>
 * Coordination: {@code {ns}:meta:ready}, {@code {ns}:lock:init}, {@code {ns}:lock:groom}
 */
public final class CacheKeyLayout {

  private static final String INDEX = ":idx";
  private static final String VALUE = ":v:";
  private static final String TMP = ":tmp:";
  private static final String META_READY = ":meta:ready";
  private static final String LOCK_INIT = ":lock:init";
  private static final String LOCK_GROOM = ":lock:groom";
  private static final String STREAM_CHANGES = ":stream:changes";

  private CacheKeyLayout() {}

  public static String liveIndexKey(CacheNamespace namespace) {
    return namespace.name() + INDEX;
  }

  public static String liveValueKey(CacheNamespace namespace, String memberId) {
    return namespace.name() + VALUE + memberId;
  }

  public static String nameAliasMemberId(String name) {
    return "n:" + name.trim().toLowerCase();
  }

  public static String tempIndexKey(CacheNamespace namespace, String runId) {
    return namespace.name() + TMP + runId + INDEX;
  }

  public static String tempValueKey(CacheNamespace namespace, String runId, String memberId) {
    return namespace.name() + TMP + runId + VALUE + memberId;
  }

  public static String l2ReadyKey(CacheNamespace namespace) {
    return namespace.name() + META_READY;
  }

  public static String initLockKey(CacheNamespace namespace) {
    return namespace.name() + LOCK_INIT;
  }

  public static String groomLockKey(CacheNamespace namespace) {
    return namespace.name() + LOCK_GROOM;
  }

  public static String changeStreamKey(CacheNamespace namespace) {
    return namespace.name() + STREAM_CHANGES;
  }
}
