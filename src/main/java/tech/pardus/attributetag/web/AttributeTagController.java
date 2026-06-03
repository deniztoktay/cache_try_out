package tech.pardus.attributetag.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.attributetag.model.AttributeTag;
import tech.pardus.attributetag.service.AttributeTagService;

@RestController
@RequestMapping("/api/attribute-tags")
public class AttributeTagController {

  private final AttributeTagService attributeTagService;

  public AttributeTagController(AttributeTagService attributeTagService) {
    this.attributeTagService = attributeTagService;
  }

  @GetMapping
  public Flux<AttributeTag> findAll() {
    return attributeTagService.findAll();
  }

  @GetMapping("/{attributeId}/{tagId}")
  public Mono<AttributeTag> getByKey(
      @PathVariable Integer attributeId, @PathVariable Integer tagId) {
    return attributeTagService.getByKey(attributeId, tagId);
  }

  @GetMapping("/by-attribute/{attributeId}")
  public Flux<AttributeTag> findByAttributeId(@PathVariable Integer attributeId) {
    return attributeTagService.findByAttributeId(attributeId);
  }

  @GetMapping("/by-tag/{tagId}")
  public Flux<AttributeTag> findByTagId(@PathVariable Integer tagId) {
    return attributeTagService.findByTagId(tagId);
  }

  @GetMapping("/exists")
  public Mono<ExistsResponse> exists(
      @RequestParam(required = false) Integer attributeId, @RequestParam(required = false) Integer tagId) {

    if (attributeId != null && tagId != null) {
      return attributeTagService
          .existsByKey(attributeId, tagId)
          .map(exists -> new ExistsResponse(exists, "attributeId+tagId"));
    }
    if (attributeId != null) {
      return attributeTagService
          .existsForAttribute(attributeId)
          .map(exists -> new ExistsResponse(exists, "attributeId"));
    }
    if (tagId != null) {
      return attributeTagService.existsForTag(tagId).map(exists -> new ExistsResponse(exists, "tagId"));
    }
    return Mono.just(new ExistsResponse(false, "none"));
  }

  public record ExistsResponse(boolean exists, String scope) {}
}
