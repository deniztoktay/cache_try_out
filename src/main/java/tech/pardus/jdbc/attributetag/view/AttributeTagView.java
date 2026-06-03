package tech.pardus.jdbc.attributetag.view;

import tech.pardus.attributetag.model.AttributeTagModel;

public record AttributeTagView(Integer attributeId, Integer tagId, String userId) {

  public static AttributeTagView fromModel(AttributeTagModel model) {
    return new AttributeTagView(model.attributeId(), model.tagId(), model.userId());
  }
}
