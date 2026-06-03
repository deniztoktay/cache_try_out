package tech.pardus.jdbc.reference.view;

import tech.pardus.reference.model.ReferenceModel;

public record ReferenceView(Integer id, String value, Integer referenceTypeId, String userId) {

  public static ReferenceView fromModel(ReferenceModel model) {
    return new ReferenceView(
        model.id(), model.value(), model.referenceTypeId(), model.userId());
  }
}
