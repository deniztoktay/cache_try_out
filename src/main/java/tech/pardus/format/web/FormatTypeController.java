package tech.pardus.format.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.format.model.FormatType;
import tech.pardus.format.service.FormatTypeService;

@RestController
@RequestMapping("/api/format-types")
public class FormatTypeController {

  private final FormatTypeService formatTypeService;

  public FormatTypeController(FormatTypeService formatTypeService) {
    this.formatTypeService = formatTypeService;
  }

  @GetMapping("/{id}")
  public Mono<FormatType> getById(@PathVariable Integer id) {
    return formatTypeService.getById(id);
  }

  @GetMapping("/lookup")
  public Mono<FormatType> lookup(
      @RequestParam String formatValue, @RequestParam(required = false) String culture) {
    return formatTypeService.getByFormatValueAndCulture(formatValue, culture);
  }

  @GetMapping
  public Flux<FormatType> findAll() {
    return formatTypeService.findAll();
  }
}
