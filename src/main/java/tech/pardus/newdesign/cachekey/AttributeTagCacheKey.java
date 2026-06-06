package tech.pardus.newdesign.cachekey;

import lombok.Getter;
import org.springframework.stereotype.Component;
import tech.pardus.newdesign.read.ReadTier;

/**
 * Attribute–tag link cache (L2 grouped id lists).
 *
 * <ul>
 *   <li>{@code v:{attributeId}} — tag ids for the attribute
 *   <li>{@code v:n:{tagId}} — attribute ids for the tag
 * </ul>
 */
@Component
public class AttributeTagCacheKey implements CacheKey {

  @Getter private final String siteName = "puurs";
  @Getter private final String key = "attributetag";

  @Override
  public ApplicationCache cache() {
    return ApplicationCache.ATTRIBUTE_TAG;
  }

  @Override
  public long initialCapacity() {
    return 0;
  }

  @Override
  public long maxCapacity() {
    return 0;
  }

  @Override
  public boolean isTemporary() {
    return false;
  }

  @Override
  public ReadTier readTier() {
    return ReadTier.L2_ONLY;
  }
}
