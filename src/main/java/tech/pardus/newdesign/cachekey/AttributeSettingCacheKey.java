package tech.pardus.newdesign.cachekey;

import lombok.Getter;
import org.springframework.stereotype.Component;
import tech.pardus.newdesign.read.ReadTier;

/**
 * Composite AttributeSetting cache ({@code attributeId} + {@code referenceTypeId}).
 *
 * <ul>
 *   <li>{@code v:{attributeId}} — list keyed by attribute
 *   <li>{@code v:n:{referenceTypeId}} — list keyed by reference type
 * </ul>
 */
@Component
public class AttributeSettingCacheKey implements CacheKey {

  @Getter private final String siteName = "puurs";
  @Getter private final String key = "attributesetting";

  @Override
  public ApplicationCache cache() {
    return ApplicationCache.ATTRIBUTE_SETTING;
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
