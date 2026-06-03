package tech.pardus.reference.cache;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.cache.read.EntityLoader;
import tech.pardus.reference.mapper.ReferenceMapper;
import tech.pardus.reference.model.ReferenceModel;
import tech.pardus.r2dbc.reference.repository.ReferenceR2dbcRepository;

@Component
public class ReferenceEntityLoader implements EntityLoader<Integer, ReferenceModel> {

  private final ReferenceR2dbcRepository repository;
  private final ReferenceMapper mapper;

  public ReferenceEntityLoader(ReferenceR2dbcRepository repository, ReferenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Mono<ReferenceModel> findById(Integer id) {
    return repository.findById(id).map(mapper::toModel);
  }

  @Override
  public Mono<List<ReferenceModel>> findAll() {
    return repository.findAll().map(mapper::toModel).collectList();
  }

  public Mono<ReferenceModel> findByReferenceTypeIdAndValue(
      Integer referenceTypeId, String value) {
    return repository
        .findByReferenceTypeIdAndValue(referenceTypeId, value)
        .map(mapper::toModel);
  }
}
