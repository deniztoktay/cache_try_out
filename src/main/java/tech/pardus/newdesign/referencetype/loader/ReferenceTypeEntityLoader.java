package tech.pardus.newdesign.referencetype.loader;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.referencetype.r2dbc.repository.ReferenceTypeR2dbcRepository;
import tech.pardus.referencetype.mapper.ReferenceTypeMapper;
import tech.pardus.referencetype.model.ReferenceTypeModel;

@Component
public class ReferenceTypeEntityLoader {

  private final ReferenceTypeR2dbcRepository repository;
  private final ReferenceTypeMapper mapper;

  public ReferenceTypeEntityLoader(
      ReferenceTypeR2dbcRepository repository, ReferenceTypeMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public Mono<ReferenceTypeModel> findById(Integer id) {
    if (id == null) {
      return Mono.empty();
    }
    return repository.findById(id).map(mapper::toModel);
  }

  public Mono<ReferenceTypeModel> findByName(String name) {
    if (name == null || name.isBlank()) {
      return Mono.empty();
    }
    return repository.findByName(name).map(mapper::toModel);
  }

  public Mono<List<ReferenceTypeModel>> findAll() {
    return repository.findAll().map(mapper::toModel).collectList();
  }
}
