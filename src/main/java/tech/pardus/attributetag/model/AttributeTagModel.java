package tech.pardus.attributetag.model;

import tech.pardus.redis.cache.Identifiable;

public record AttributeTagModel(Integer attributeId, Integer tagId, String userId)
    implements Identifiable<String> {

  @Override
  public String getId() {
    return getStringId();
  }

  @Override
  public String getStringId() {
    return AttributeTagKeys.memberId(attributeId, tagId);
  }
}
