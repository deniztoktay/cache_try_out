package tech.pardus.newdesign.attributetag.view;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.attributetag.mapper.AttributeTagMapper;
import tech.pardus.attributetag.model.AttributeTag;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.attribute.model.Attribute;
import tech.pardus.newdesign.attribute.view.AttributeViewService;
import tech.pardus.newdesign.attributetag.jdbc.entity.AttributeTagEntity;
import tech.pardus.newdesign.attributetag.jdbc.entity.AttributeTagId;
import tech.pardus.newdesign.attributetag.jdbc.repository.AttributeTagJpaRepository;
import tech.pardus.newdesign.attributetag.loader.AttributeTagEntityLoader;
import tech.pardus.newdesign.attributetag.model.AttributeTagView;
import tech.pardus.newdesign.read.GroupedCollectionReadStrategy;
import tech.pardus.newdesign.tag.view.TagViewService;
import tech.pardus.tag.model.Tag;

@Service
public class AttributeTagViewService {

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  private final GroupedCollectionReadStrategy<Integer> idListReadStrategy;
  private final AttributeTagEntityLoader loader;
  private final TagViewService tagViewService;
  private final AttributeViewService attributeViewService;
  private final AttributeTagJpaRepository jpaRepository;
  private final AttributeTagMapper mapper;

  public AttributeTagViewService(
      GroupedCollectionReadStrategy<Integer> attributeTagIdListReadStrategy,
      AttributeTagEntityLoader loader,
      TagViewService tagViewService,
      AttributeViewService attributeViewService,
      AttributeTagJpaRepository jpaRepository,
      AttributeTagMapper mapper) {
    this.idListReadStrategy = attributeTagIdListReadStrategy;
    this.loader = loader;
    this.tagViewService = tagViewService;
    this.attributeViewService = attributeViewService;
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  /** Tag ids for an attribute from L2 ({@code v:{attributeId}}) then DB. */
  public Mono<List<Integer>> findTagIdsByAttributeId(Integer attributeId) {
    if (attributeId == null) {
      return Mono.just(List.of());
    }
    return idListReadStrategy.findByValueGroup(
        String.valueOf(attributeId), loader.findTagIdsByAttributeId(attributeId));
  }

  /** Attribute ids for a tag from L2 ({@code v:n:{tagId}}) then DB. */
  public Mono<List<Integer>> findAttributeIdsByTagId(Integer tagId) {
    if (tagId == null) {
      return Mono.just(List.of());
    }
    return idListReadStrategy.findByNamedGroup(
        String.valueOf(tagId), loader.findAttributeIdsByTagId(tagId));
  }

  /** Tags linked to an attribute: ids from cache, entities from {@link TagViewService}. */
  public Flux<Tag> findTagsByAttributeId(Integer attributeId) {
    return findTagIdsByAttributeId(attributeId)
        .flatMapMany(ids -> Flux.fromIterable(ids).flatMap(tagViewService::getById));
  }

  /** Attributes linked to a tag: ids from cache, entities from {@link AttributeViewService}. */
  public Flux<Attribute> findAttributesByTagId(Integer tagId) {
    return findAttributeIdsByTagId(tagId)
        .flatMapMany(ids -> Flux.fromIterable(ids).flatMap(attributeViewService::getById));
  }

  public Mono<AttributeTag> getByKey(Integer attributeId, Integer tagId) {
    if (attributeId == null || tagId == null) {
      return Mono.empty();
    }
    return findTagIdsByAttributeId(attributeId)
        .flatMap(
            tagIds ->
                tagIds.contains(tagId)
                    ? Mono.fromCallable(
                            () -> jpaRepository.findById(new AttributeTagId(attributeId, tagId)))
                        .flatMap(
                            opt ->
                                opt.map(entity -> Mono.just(mapper.toDomain(entity)))
                                    .orElseGet(() -> loader.findByKey(attributeId, tagId).map(mapper::toDomain)))
                    : Mono.empty());
  }

  public Flux<AttributeTag> findAllLinks() {
    return loader
        .findAll()
        .flatMapMany(models -> Flux.fromIterable(models).concatMap(this::modelToDomainWithTimestamps));
  }

  public Optional<AttributeTagView> findByKey(Integer attributeId, Integer tagId) {
    if (attributeId == null || tagId == null) {
      return Optional.empty();
    }
    return loader
        .findByKey(attributeId, tagId)
        .map(AttributeTagView::fromModel)
        .blockOptional(READ_TIMEOUT);
  }

  public List<AttributeTagView> findAllForValidation() {
    return loader.findAll().map(models -> models.stream().map(AttributeTagView::fromModel).toList()).block(READ_TIMEOUT);
  }

  public boolean existsByAttributeIdAndTagId(Integer attributeId, Integer tagId) {
    if (attributeId == null || tagId == null) {
      return false;
    }
    return findTagIdsByAttributeId(attributeId)
        .map(ids -> ids.contains(tagId))
        .blockOptional(READ_TIMEOUT)
        .orElse(false);
  }

  public boolean existsForAttribute(Integer attributeId) {
    if (attributeId == null) {
      return false;
    }
    return findAllForValidation().stream()
        .anyMatch(link -> Objects.equals(link.attributeId(), attributeId));
  }

  public boolean existsForTag(Integer tagId) {
    if (tagId == null) {
      return false;
    }
    return findAllForValidation().stream().anyMatch(link -> Objects.equals(link.tagId(), tagId));
  }

  private Mono<AttributeTag> modelToDomainWithTimestamps(AttributeTagModel model) {
    return Mono.fromCallable(
            () -> jpaRepository.findById(new AttributeTagId(model.attributeId(), model.tagId())))
        .flatMap(
            opt ->
                opt.map(entity -> Mono.just(mapper.toDomain(entity)))
                    .orElseGet(() -> Mono.just(mapper.toDomain(model))));
  }
}
