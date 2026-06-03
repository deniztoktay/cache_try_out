package tech.pardus.attribute.model;

import lombok.Builder;
import tech.pardus.redis.cache.CacheKeyLayout;
import tech.pardus.redis.cache.Identifiable;

@Builder
public record AttributeModel(
    Integer id,
    String name,
    String description,
    String type,
    Boolean isGmp,
    Boolean showToAdmin,
    Boolean showToUser) implements Identifiable<Integer>{
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
