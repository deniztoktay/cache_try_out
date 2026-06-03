package tech.pardus.format.cache;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.pardus.cache.read.EntityLoader;
import tech.pardus.format.mapper.FormatTypeMapper;
import tech.pardus.format.model.FormatTypeModel;
import tech.pardus.r2dbc.format.repository.FormatTypeR2dbcRepository;

@Component
public class FormatTypeEntityLoader implements EntityLoader<Integer, FormatTypeModel> {

  private final FormatTypeR2dbcRepository repository;
  private final FormatTypeMapper mapper;

  public FormatTypeEntityLoader(FormatTypeR2dbcRepository repository, FormatTypeMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Mono<FormatTypeModel> findById(Integer id) {
    return repository.findById(id).map(mapper::toModel);
  }

  @Override
  public Mono<List<FormatTypeModel>> findAll() {
    return repository.findAll().map(mapper::toModel).collectList();
  }

  public Mono<FormatTypeModel> findByFormatValueAndCulture(String formatValue, String culture) {
    return repository.findByFormatValueAndCulture(formatValue, culture).map(mapper::toModel);
  }
}
