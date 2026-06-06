package tech.pardus.newdesign.attribute.loader;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.attribute.mapper.AttributeMapper;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.newdesign.attribute.r2dbc.repository.AttributeR2dbcRepository;

@Component
public class AttributeEntityLoader {

  private final AttributeR2dbcRepository repository;
  private final AttributeMapper mapper;

  public AttributeEntityLoader(AttributeR2dbcRepository repository, AttributeMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public Mono<AttributeModel> findById(Integer id) {
    if (id == null) {
      return Mono.empty();
    }
    return repository.findById(id).map(mapper::toModel);
  }

  public Mono<AttributeModel> findByName(String name) {
    if (name == null || name.isBlank()) {
      return Mono.empty();
    }
    return repository.findByName(name).map(mapper::toModel);
  }

  public Mono<List<AttributeModel>> findAll() {
    return repository.findAll().map(mapper::toModel).collectList();
  }
}
