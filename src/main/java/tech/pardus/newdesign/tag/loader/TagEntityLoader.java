package tech.pardus.newdesign.tag.loader;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.tag.r2dbc.repository.TagR2dbcRepository;
import tech.pardus.tag.mapper.TagMapper;
import tech.pardus.tag.model.TagModel;

@Component
public class TagEntityLoader {

  private final TagR2dbcRepository repository;
  private final TagMapper tagMapper;

  public TagEntityLoader(TagR2dbcRepository repository, TagMapper tagMapper) {
    this.repository = repository;
    this.tagMapper = tagMapper;
  }

  public Mono<TagModel> findById(Integer id) {
    if (id == null) {
      return Mono.empty();
    }
    return repository.findById(id).map(tagMapper::toModel);
  }

  public Mono<TagModel> findByName(String name) {
    if (name == null || name.isBlank()) {
      return Mono.empty();
    }
    return repository.findByName(name).map(tagMapper::toModel);
  }

  public Mono<List<TagModel>> findAll() {
    return repository.findAll().map(tagMapper::toModel).collectList();
  }
}
