package tech.pardus.newdesign.attribute.model;

import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.newdesign.attribute.r2dbc.entity.AttributeRecord;

/** Read-only attribute row for validation against uk_attribute. */
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
