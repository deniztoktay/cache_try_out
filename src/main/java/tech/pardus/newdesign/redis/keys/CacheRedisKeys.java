package tech.pardus.newdesign.redis.keys;

import tech.pardus.newdesign.cachekey.CacheKey;

/**
 * Redis key layout for every cache partition ({@code {site}:cache:{name}:...}).
 *
 * <pre>
 * {root}:meta              bulk-load ready marker
 * {root}:idx               set of entity ids
 * {root}:v:{id}            single value OR grouped collection (e.g. by attributeId)
 * {root}:v:n:{alias}       named lookup (tag name) OR alternate group (e.g. referenceTypeId)
 * </pre>
 */
public final class CacheRedisKeys {

  private static final String META = ":meta";
  private static final String INDEX = ":idx";
  private static final String VALUE = ":v:";
  private static final String NAMED = ":v:n:";

  private CacheRedisKeys() {}

  /** Example: {@code puurs:cache:tag:meta}. */
  public static String metaKey(CacheKey cache) {
    return requireCache(cache).getParentKey() + META;
  }

  /** Example: {@code puurs:cache:tag:idx}. */
  public static String indexKey(CacheKey cache) {
    return requireCache(cache).getParentKey() + INDEX;
  }

  /** Example: {@code puurs:cache:tag:v:1} or {@code puurs:cache:attributesetting:v:42}. */
  public static String valueKey(CacheKey cache, String memberId) {
    return requireCache(cache).getParentKey() + VALUE + requireMember(memberId);
  }

  /**
   * Example: {@code puurs:cache:tag:v:n:Tag1} or {@code puurs:cache:attributesetting:v:n:2}
   * (referenceTypeId group).
   */
  public static String namedKey(CacheKey cache, String alias) {
    return requireCache(cache).getParentKey() + NAMED + requireMember(alias);
  }

  private static CacheKey requireCache(CacheKey cache) {
    if (cache == null) {
      throw new IllegalArgumentException("cache is required");
    }
    return cache;
  }

  private static String requireMember(String member) {
    if (member == null || member.isBlank()) {
      throw new IllegalArgumentException("member id or alias is required");
    }
    return member;
  }
}
