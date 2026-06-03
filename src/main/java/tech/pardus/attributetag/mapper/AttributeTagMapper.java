package tech.pardus.attributetag.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tech.pardus.attributetag.model.AttributeTag;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.jdbc.attribute.entity.AttributeTagEntity;
import tech.pardus.r2dbc.attribute.entity.AttributeTagRecord;

@Mapper(componentModel = "spring")
public interface AttributeTagMapper {

  @Mapping(target = "attributeId", source = "id.attributeId")
  @Mapping(target = "tagId", source = "id.tagId")
  @Mapping(target = "userId", ignore = true)
  AttributeTagModel toModel(AttributeTagRecord record);

  AttributeTagModel toModel(AttributeTagEntity entity);

  default AttributeTag toDomain(AttributeTagModel model) {
    return new AttributeTag(model.attributeId(), model.tagId(), model.userId(), null, null);
  }

  List<AttributeTag> toDomains(List<AttributeTagModel> models);

  default AttributeTag toDomain(AttributeTagEntity entity) {
    return new AttributeTag(
        entity.getAttributeId(),
        entity.getTagId(),
        entity.getUserId(),
        instantToLocal(entity.getCreateTime()),
        instantToLocal(entity.getModifyTime()));
  }

  default LocalDateTime instantToLocal(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
