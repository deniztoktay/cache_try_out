package tech.pardus.jdbc.format.view;

import tech.pardus.format.model.FormatTypeModel;

public record FormatTypeView(
    Integer id, String formatValue, String description, String type, String culture) {

  public static FormatTypeView fromModel(FormatTypeModel model) {
    return new FormatTypeView(
        model.id(), model.formatValue(), model.description(), model.type(), model.culture());
  }
}
