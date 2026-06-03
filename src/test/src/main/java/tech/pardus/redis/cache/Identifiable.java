package tech.pardus.redis.cache;

/**
 * Cache entities addressable by a typed id and a stable Redis/index string id.
 *
 * <p>{@link #getStringId()} is used for L1 keys, Redis value keys ({@code {ns}:v:{id}}), and SET
 * index members. It must be unique within a {@link CacheNamespace} and safe as a Redis key suffix.
 */
public interface Identifiable<ID> {

  ID getId();

  String getStringId();
}
