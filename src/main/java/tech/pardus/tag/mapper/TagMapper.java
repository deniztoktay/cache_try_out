package tech.pardus.tag.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import tech.pardus.r2dbc.tag.entity.TagRecord;
import tech.pardus.tag.model.Tag;
import tech.pardus.tag.model.TagModel;

@Mapper(componentModel = "spring")
public interface TagMapper {

  TagModel toModel(TagRecord record);

  List<TagModel> toModels(List<TagRecord> records);

  Tag toDomain(TagRecord record);

  Tag toDomain(TagModel model);

  TagModel toModel(Tag tag);

  List<Tag> toDomains(List<TagModel> models);
}
