package tech.pardus.newdesign.tag.view;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.newdesign.read.L1L2SingleValueReadStrategy;
import tech.pardus.newdesign.tag.loader.TagEntityLoader;
import tech.pardus.newdesign.tag.model.TagView;
import tech.pardus.tag.mapper.TagMapper;
import tech.pardus.tag.model.Tag;
import tech.pardus.tag.model.TagModel;

@Service
public class TagViewService {

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  private final L1L2SingleValueReadStrategy<TagModel> readStrategy;
  private final TagEntityLoader loader;
  private final TagMapper tagMapper;

  public TagViewService(
      L1L2SingleValueReadStrategy<TagModel> tagL1L2ReadStrategy,
      TagEntityLoader loader,
      TagMapper tagMapper) {
    this.readStrategy = tagL1L2ReadStrategy;
    this.loader = loader;
    this.tagMapper = tagMapper;
  }

  public Mono<Tag> getById(Integer id) {
    if (id == null) {
      return Mono.empty();
    }
    return readStrategy
        .findById(String.valueOf(id), loader.findById(id))
        .flatMap(opt -> opt.map(tagMapper::toDomain).map(Mono::just).orElseGet(Mono::empty));
  }

  public Mono<Tag> getByName(String name) {
    if (name == null || name.isBlank()) {
      return Mono.empty();
    }
    return readStrategy.findByName(name, loader.findByName(name)).map(tagMapper::toDomain);
  }

  public Flux<Tag> findAll() {
    return readStrategy
        .find(model -> true, loader.findAll())
        .flatMapMany(models -> Flux.fromIterable(tagMapper.toDomains(models)));
  }

  public Flux<Tag> findAssignable() {
    return findAll().filter(tag -> Boolean.TRUE.equals(tag.canUserAssign()));
  }

  public Optional<TagView> findById(Integer id) {
    if (id == null) {
      return Optional.empty();
    }
    return readStrategy
        .findById(String.valueOf(id), loader.findById(id))
        .map(opt -> opt.map(TagView::fromModel))
        .blockOptional(READ_TIMEOUT)
        .flatMap(o -> o);
  }

  public List<TagView> findAllForValidation() {
    return readStrategy
        .find(model -> true, loader.findAll())
        .map(models -> models.stream().map(TagView::fromModel).toList())
        .block(READ_TIMEOUT);
  }

  public boolean existsByNameAndType(String name, String type, Integer excludeId) {
    if (name == null || type == null) {
      return false;
    }
    return findAllForValidation().stream()
        .filter(tag -> excludeId == null || !Objects.equals(tag.id(), excludeId))
        .anyMatch(tag -> name.equals(tag.name()) && type.equals(tag.type()));
  }
}
