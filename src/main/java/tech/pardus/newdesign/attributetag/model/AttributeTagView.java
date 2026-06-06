package tech.pardus.newdesign.attributetag.model;

import tech.pardus.attributetag.model.AttributeTagModel;

public record AttributeTagView(Integer attributeId, Integer tagId, String userId) {

  public static AttributeTagView fromModel(AttributeTagModel model) {
    return new AttributeTagView(model.attributeId(), model.tagId(), model.userId());
  }
}
