package tech.pardus.format.model;

import tech.pardus.redis.cache.Identifiable;

public record FormatTypeModel(
    Integer id, String formatValue, String description, String type, String culture)
    implements Identifiable<Integer> {

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public String getStringId() {
    return String.valueOf(id);
  }

  public String valueCultureAliasStringId() {
    return FormatTypeKeys.valueCultureAliasMemberId(formatValue, culture);
  }
}
