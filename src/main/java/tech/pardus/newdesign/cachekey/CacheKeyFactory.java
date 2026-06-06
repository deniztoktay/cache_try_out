package tech.pardus.newdesign.cachekey;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Resolves {@link CacheKey} components by {@link ApplicationCache}. */
@Component
public class CacheKeyFactory {

  private final Map<ApplicationCache, CacheKey> keysByCache;

  public CacheKeyFactory(List<CacheKey> cacheKeys) {
    this.keysByCache =
        cacheKeys.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    CacheKey::cache, Function.identity(), this::duplicateCacheKey));
  }

  public CacheKey get(ApplicationCache cache) {
    var key = keysByCache.get(cache);
    if (key == null) {
      throw new IllegalArgumentException("No CacheKey component registered for " + cache);
    }
    return key;
  }

  private CacheKey duplicateCacheKey(CacheKey left, CacheKey right) {
    throw new IllegalStateException(
        "Duplicate CacheKey registration for "
            + left.cache()
            + ": "
            + left.getClass().getName()
            + " and "
            + right.getClass().getName());
  }
}
