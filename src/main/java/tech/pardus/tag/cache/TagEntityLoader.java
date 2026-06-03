package tech.pardus.tag.cache;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.cache.read.EntityLoader;
import tech.pardus.r2dbc.tag.repository.TagR2dbcRepository;
import tech.pardus.tag.mapper.TagMapper;
import tech.pardus.tag.model.TagModel;

@Component
public class TagEntityLoader implements EntityLoader<Integer, TagModel> {

  private final TagR2dbcRepository repository;
  private final TagMapper tagMapper;

  public TagEntityLoader(TagR2dbcRepository repository, TagMapper tagMapper) {
    this.repository = repository;
    this.tagMapper = tagMapper;
  }

  @Override
  public Mono<TagModel> findById(Integer id) {
    return repository.findById(id).map(tagMapper::toModel);
  }

  @Override
  public Mono<List<TagModel>> findAll() {
    return repository.findAll().map(tagMapper::toModel).collectList();
  }
}
