package tech.pardus.newdesign.referencetype.model;

import tech.pardus.newdesign.referencetype.r2dbc.entity.ReferenceTypeRecord;
import tech.pardus.referencetype.model.ReferenceTypeModel;

/** Read-only reference type row for validation. */
public record ReferenceTypeView(Integer id, String name, Boolean showToUi, Integer priority) {

  public static ReferenceTypeView fromModel(ReferenceTypeModel model) {
    return new ReferenceTypeView(model.id(), model.name(), model.showToUi(), model.priority());
  }

  public static ReferenceTypeView fromRecord(ReferenceTypeRecord record) {
    return new ReferenceTypeView(record.id(), record.name(), record.showToUi(), record.priority());
  }
}
