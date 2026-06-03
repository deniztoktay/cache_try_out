package tech.pardus.r2dbc.format.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import tech.pardus.r2dbc.format.entity.FormatTypeRecord;

public interface FormatTypeR2dbcRepository
    extends ReactiveCrudRepository<FormatTypeRecord, Integer> {

  Mono<FormatTypeRecord> findByFormatValueAndCulture(String formatValue, String culture);
}
