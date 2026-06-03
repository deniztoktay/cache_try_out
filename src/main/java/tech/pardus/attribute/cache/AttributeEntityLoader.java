package tech.pardus.attribute.cache;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.attribute.mapper.AttributeMapper;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.cache.read.EntityLoader;
import tech.pardus.r2dbc.attribute.repository.AttributeR2dbcRepository;

@Component
public class AttributeEntityLoader implements EntityLoader<Integer, AttributeModel> {

  private final AttributeR2dbcRepository repository;
  private final AttributeMapper mapper;

  public AttributeEntityLoader(AttributeR2dbcRepository repository, AttributeMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Mono<AttributeModel> findById(Integer id) {
    return repository.findById(id).map(mapper::toModel);
  }

  @Override
  public Mono<List<AttributeModel>> findAll() {
    return repository.findAll().map(mapper::toModel).collectList();
  }
}
