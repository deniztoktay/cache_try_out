package tech.pardus.attributetag.model;

import tech.pardus.r2dbc.attribute.entity.AttributeTagKey;

public final class AttributeTagKeys {

  private AttributeTagKeys() {}

  public static String memberId(Integer attributeId, Integer tagId) {
    if (attributeId == null || tagId == null) {
      throw new IllegalArgumentException("attributeId and tagId are required");
    }
    return attributeId + ":" + tagId;
  }

  public static AttributeTagKey toKey(String memberId) {
    var parts = memberId.split(":", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid AttributeTag member id: " + memberId);
    }
    return new AttributeTagKey(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
  }
}
