package tech.pardus.newdesign.cachekey;

import lombok.Getter;
import org.springframework.stereotype.Component;
import tech.pardus.newdesign.read.ReadTier;

@Component
public class TagCacheKey implements CacheKey {

  @Getter private final String siteName = "puurs";
  @Getter private final String key = "tag";

  @Override
  public ApplicationCache cache() {
    return ApplicationCache.TAG;
  }

  @Override
  public long initialCapacity() {
    return 10;
  }

  @Override
  public long maxCapacity() {
    return 100;
  }

  @Override
  public boolean isTemporary() {
    return false;
  }

  @Override
  public ReadTier readTier() {
    return ReadTier.L1_L2;
  }
}
