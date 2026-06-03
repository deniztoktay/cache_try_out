package tech.pardus.format.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;
import tech.pardus.format.model.FormatType;
import tech.pardus.format.model.FormatTypeModel;
import tech.pardus.jdbc.format.entity.FormatTypeEntity;
import tech.pardus.r2dbc.format.entity.FormatTypeRecord;

@Mapper(componentModel = "spring")
public interface FormatTypeMapper {

  FormatTypeModel toModel(FormatTypeRecord record);

  FormatTypeModel toModel(FormatTypeEntity entity);

  FormatType toDomain(FormatTypeModel model);

  List<FormatType> toDomains(List<FormatTypeModel> models);

  default FormatType toDomain(FormatTypeEntity entity) {
    return new FormatType(
        entity.getId(),
        entity.getFormatValue(),
        entity.getDescription(),
        entity.getType(),
        entity.getCulture(),
        instantToLocal(entity.getCreateTime()),
        instantToLocal(entity.getModifyTime()));
  }

  default LocalDateTime instantToLocal(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
