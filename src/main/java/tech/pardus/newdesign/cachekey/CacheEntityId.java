package tech.pardus.newdesign.cachekey;

/** Builds and parses cache entity ids shared by L1 and L2 (no name aliases). */
public final class CacheEntityId {

  private static final String SEPARATOR = ":";

  private CacheEntityId() {}

  /** Composite id: {@code 1:42} or {@code rt:5:attr:10}. */
  public static String composite(Object... parts) {
    if (parts == null || parts.length == 0) {
      throw new IllegalArgumentException("At least one id part is required");
    }
    var sb = new StringBuilder();
    for (var i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        throw new IllegalArgumentException("Id part at index " + i + " is null");
      }
      if (i > 0) {
        sb.append(SEPARATOR);
      }
      sb.append(parts[i]);
    }
    return sb.toString();
  }

  public static String[] split(String id) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id is required");
    }
    return id.split(SEPARATOR, -1);
  }

  /** Member id when collection is keyed by attributeId ({@code attributeId:referenceTypeId}). */
  public static String attributeSettingByAttribute(Integer attributeId, Integer referenceTypeId) {
    return composite(attributeId, referenceTypeId);
  }

  /** Member id when collection is keyed by referenceTypeId ({@code referenceTypeId:attributeId}). */
  public static String attributeSettingByReferenceType(
      Integer referenceTypeId, Integer attributeId) {
    return composite(referenceTypeId, attributeId);
  }

  /** Member id for idx ({@code attributeId:tagId}). */
  public static String attributeTagMember(Integer attributeId, Integer tagId) {
    return composite(attributeId, tagId);
  }
}
