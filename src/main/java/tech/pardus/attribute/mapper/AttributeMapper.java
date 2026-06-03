package tech.pardus.attribute.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import tech.pardus.attribute.model.Attribute;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.jdbc.attribute.entity.AttributeEntity;
import tech.pardus.r2dbc.attribute.entity.AttributeRecord;

@Mapper(componentModel = "spring")
public interface AttributeMapper {

  AttributeModel toModel(AttributeRecord record);

  AttributeModel toModel(AttributeEntity entity);

  Attribute toDomain(AttributeRecord record);

  Attribute toDomain(AttributeModel model);

  AttributeEntity toEntity(AttributeModel model);

  List<Attribute> toDomains(List<AttributeModel> models);
}
