package tech.pardus.newdesign.tag.model;

import tech.pardus.newdesign.tag.r2dbc.entity.TagRecord;
import tech.pardus.tag.model.TagModel;

/** Read-only tag row for validation against Uk_Tags. */
public record TagView(
    Integer id, String name, String type, TagUsageType usageType, Boolean canUserAssign) {

  public static TagView fromRecord(TagRecord record) {
    return fromFields(
        record.id(), record.name(), record.type(), record.usageType(), record.canUserAssign());
  }

  public static TagView fromModel(TagModel model) {
    return fromFields(model.id(), model.name(), model.type(), model.usageType(), model.canUserAssign());
  }

  private static TagView fromFields(
      Integer id, String name, String type, String usageTypeRaw, Boolean canUserAssign) {
    TagUsageType usage = null;
    if (usageTypeRaw != null && !usageTypeRaw.isBlank()) {
      usage = TagUsageType.valueOf(usageTypeRaw.trim().toUpperCase());
    }
    return new TagView(id, name, type, usage, canUserAssign);
  }
}
