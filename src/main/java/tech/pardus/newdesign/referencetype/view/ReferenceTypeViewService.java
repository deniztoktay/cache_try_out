package tech.pardus.newdesign.referencetype.view;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.read.L1L2SingleValueReadStrategy;
import tech.pardus.newdesign.referencetype.loader.ReferenceTypeEntityLoader;
import tech.pardus.newdesign.referencetype.model.ReferenceTypeView;
import tech.pardus.referencetype.mapper.ReferenceTypeMapper;
import tech.pardus.referencetype.model.ReferenceType;
import tech.pardus.referencetype.model.ReferenceTypeModel;

@Service
public class ReferenceTypeViewService {

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  private final L1L2SingleValueReadStrategy<ReferenceTypeModel> readStrategy;
  private final ReferenceTypeEntityLoader loader;
  private final ReferenceTypeMapper mapper;

  public ReferenceTypeViewService(
      L1L2SingleValueReadStrategy<ReferenceTypeModel> referenceTypeL1L2ReadStrategy,
      ReferenceTypeEntityLoader loader,
      ReferenceTypeMapper mapper) {
    this.readStrategy = referenceTypeL1L2ReadStrategy;
    this.loader = loader;
    this.mapper = mapper;
  }

  public Mono<ReferenceType> getById(Integer id) {
    if (id == null) {
      return Mono.empty();
    }
    return readStrategy
        .findById(String.valueOf(id), loader.findById(id))
        .flatMap(opt -> opt.map(mapper::toDomain).map(Mono::just).orElseGet(Mono::empty));
  }

  public Mono<ReferenceType> getByName(String name) {
    if (name == null || name.isBlank()) {
      return Mono.empty();
    }
    return readStrategy.findByName(name, loader.findByName(name)).map(mapper::toDomain);
  }

  public Flux<ReferenceType> findAll() {
    return readStrategy
        .find(model -> true, loader.findAll())
        .flatMapMany(models -> Flux.fromIterable(mapper.toDomains(models)));
  }

  public Flux<ReferenceType> findShowToUi() {
    return findAll().filter(type -> Boolean.TRUE.equals(type.showToUi()));
  }

  public Optional<ReferenceTypeView> findById(Integer id) {
    if (id == null) {
      return Optional.empty();
    }
    return readStrategy
        .findById(String.valueOf(id), loader.findById(id))
        .map(opt -> opt.map(ReferenceTypeView::fromModel))
        .blockOptional(READ_TIMEOUT)
        .flatMap(o -> o);
  }

  public List<ReferenceTypeView> findAllForValidation() {
    return readStrategy
        .find(model -> true, loader.findAll())
        .map(models -> models.stream().map(ReferenceTypeView::fromModel).toList())
        .block(READ_TIMEOUT);
  }

  public boolean existsByName(String name, Integer excludeId) {
    if (name == null) {
      return false;
    }
    return findAllForValidation().stream()
        .filter(type -> excludeId == null || !Objects.equals(type.id(), excludeId))
        .anyMatch(type -> name.equals(type.name()));
  }
}
