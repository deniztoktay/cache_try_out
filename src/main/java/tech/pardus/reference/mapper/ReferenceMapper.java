package tech.pardus.reference.mapper;

import org.mapstruct.Mapper;
import tech.pardus.jdbc.reference.entity.ReferenceEntity;
import tech.pardus.r2dbc.reference.entity.ReferenceRecord;
import tech.pardus.reference.model.ReferenceModel;

@Mapper(componentModel = "spring")
public interface ReferenceMapper {

  ReferenceModel toModel(ReferenceRecord record);

  ReferenceModel toModel(ReferenceEntity entity);
}
