package tech.pardus.attribute.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.attribute.model.Attribute;
import tech.pardus.attribute.service.AttributeService;

@RestController
@RequestMapping("/api/attributes")
public class AttributeController {

  private final AttributeService attributeService;

  public AttributeController(AttributeService attributeService) {
    this.attributeService = attributeService;
  }

  @GetMapping("/{id}")
  public Mono<Attribute> getById(@PathVariable Integer id) {
    return attributeService.getById(id);
  }

  @GetMapping
  public Flux<Attribute> findAll() {
    return attributeService.findAll();
  }
}
