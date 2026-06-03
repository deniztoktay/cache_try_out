package tech.pardus.format.model;

public final class FormatTypeKeys {

  private FormatTypeKeys() {}

  /** Secondary L2 key for {@code Uk_FormatType} (FormatValue, culture). */
  public static String valueCultureAliasMemberId(String formatValue, String culture) {
    var value = formatValue == null ? "" : formatValue.trim().toLowerCase();
    var cult = culture == null ? "" : culture.trim().toLowerCase();
    return "fc:" + value + ":" + cult;
  }
}
