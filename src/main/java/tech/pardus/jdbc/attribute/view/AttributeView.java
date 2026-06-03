package tech.pardus.jdbc.attribute.view;

import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.r2dbc.attribute.entity.AttributeRecord;

public record AttributeView(
    Integer id,
    String name,
    String description,
    String type,
    Boolean isGmp,
    Boolean showToAdmin,
    String operantUser,
    Boolean showToUser) {

  public static AttributeView fromModel(AttributeModel model) {
    return new AttributeView(
        model.id(),
        model.name(),
        model.description(),
        model.type(),
        model.isGmp(),
        model.showToAdmin(),
       null,
        model.showToUser());
  }

  public static AttributeView fromRecord(AttributeRecord record) {
    return new AttributeView(
        record.id(),
        record.name(),
        record.description(),
        record.type(),
        record.isGmp(),
        record.showToAdmin(),
        record.operantUser(),
        record.showToUser());
  }
}
