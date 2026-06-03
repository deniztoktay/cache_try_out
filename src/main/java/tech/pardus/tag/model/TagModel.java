package tech.pardus.tag.model;

import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.Identifiable;

/** Cache persistence model for tags (same shape as {@link Tag} and {@link tech.pardus.r2dbc.tag.entity.TagRecord}). */
public record TagModel(
    Integer id, String name, String type, String usageType, Boolean canUserAssign)
    implements Identifiable<Integer> {

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public String getStringId() {
    return String.valueOf(id);
  }

  /** Secondary Redis/L1 key for name-based lookups. */
  public String nameAliasStringId() {
    return CacheKeyLayout.nameAliasMemberId(name);
  }
}
