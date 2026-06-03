package tech.pardus.tag.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.tag.model.Tag;
import tech.pardus.tag.service.TagService;

@RestController
@RequestMapping("/api/tags")
public class TagController {
  private final TagService tagService;

  public TagController(TagService tagService) {
    this.tagService = tagService;
  }

  @GetMapping
  public Flux<Tag> findAll() {
    return tagService.findAll();
  }

  @GetMapping("/assignable")
  public Flux<Tag> findAssignable() {
    return tagService.findAssignable();
  }

  @GetMapping("/{id}")
  public Mono<Tag> getById(@PathVariable Integer id) {
    return tagService.getById(id);
  }

  @GetMapping("/by-name/{name}")
  public Mono<Tag> getByName(@PathVariable String name) {
    return tagService.getByName(name);
  }
}
