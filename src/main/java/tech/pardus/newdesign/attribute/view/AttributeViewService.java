package tech.pardus.newdesign.attribute.view;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.attribute.mapper.AttributeMapper;
import tech.pardus.attribute.model.Attribute;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.newdesign.attribute.loader.AttributeEntityLoader;
import tech.pardus.newdesign.attribute.model.AttributeView;
import tech.pardus.newdesign.attribute.r2dbc.repository.AttributeR2dbcRepository;
import tech.pardus.newdesign.read.L1L2SingleValueReadStrategy;

@Service
public class AttributeViewService {

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  private final L1L2SingleValueReadStrategy<AttributeModel> readStrategy;
  private final AttributeEntityLoader loader;
  private final AttributeR2dbcRepository repository;
  private final AttributeMapper attributeMapper;

  public AttributeViewService(
      L1L2SingleValueReadStrategy<AttributeModel> attributeL1L2ReadStrategy,
      AttributeEntityLoader loader,
      AttributeR2dbcRepository repository,
      AttributeMapper attributeMapper) {
    this.readStrategy = attributeL1L2ReadStrategy;
    this.loader = loader;
    this.repository = repository;
    this.attributeMapper = attributeMapper;
  }

  public Mono<Attribute> getById(Integer id) {
    if (id == null) {
      return Mono.empty();
    }
    return readStrategy
        .findById(String.valueOf(id), loader.findById(id))
        .flatMap(
            opt ->
                opt.map(this::modelToDomainWithTimestamps).orElseGet(Mono::empty));
  }

  public Mono<Attribute> getByName(String name) {
    if (name == null || name.isBlank()) {
      return Mono.empty();
    }
    return readStrategy
        .findByName(name, loader.findByName(name))
        .flatMap(this::modelToDomainWithTimestamps);
  }

  public Flux<Attribute> findAll() {
    return readStrategy
        .find(model -> true, loader.findAll())
        .flatMapMany(models -> Flux.fromIterable(models).concatMap(this::modelToDomainWithTimestamps));
  }

  public Optional<AttributeView> findById(Integer id) {
    if (id == null) {
      return Optional.empty();
    }
    return readStrategy
        .findById(String.valueOf(id), loader.findById(id))
        .map(opt -> opt.map(AttributeView::fromModel))
        .blockOptional(READ_TIMEOUT)
        .flatMap(o -> o);
  }

  public List<AttributeView> findAllForValidation() {
    return readStrategy
        .find(model -> true, loader.findAll())
        .map(models -> models.stream().map(AttributeView::fromModel).toList())
        .block(READ_TIMEOUT);
  }

  /** Uk_attribute on {@code Name}. */
  public boolean existsByName(String name, Integer excludeId) {
    if (name == null) {
      return false;
    }
    return findAllForValidation().stream()
        .filter(attr -> excludeId == null || !Objects.equals(attr.id(), excludeId))
        .anyMatch(attr -> name.equals(attr.name()));
  }

  private Mono<Attribute> modelToDomainWithTimestamps(AttributeModel model) {
    if (model.id() == null) {
      return Mono.just(attributeMapper.toDomain(model));
    }
    return repository
        .findById(model.id())
        .map(attributeMapper::toDomain)
        .defaultIfEmpty(attributeMapper.toDomain(model));
  }
}
