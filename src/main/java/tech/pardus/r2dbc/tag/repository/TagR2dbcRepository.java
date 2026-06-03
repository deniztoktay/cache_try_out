package tech.pardus.r2dbc.tag.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pardus.r2dbc.tag.entity.TagRecord;

public interface TagR2dbcRepository extends ReactiveCrudRepository<TagRecord, Integer> {

  Mono<TagRecord> findByName(String name);

  Flux<TagRecord> findByCanUserAssignTrueOrderByNameAsc();

  @Query(
      """
      SELECT * FROM lah."Tag"
      WHERE (:name IS NULL OR LOWER("Name") LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:assignable IS NULL OR "CanUserAssign" = :assignable)
      ORDER BY "Name"
      """)
  Flux<TagRecord> findFiltered(
      @Param("name") String name, @Param("assignable") Boolean assignable, Pageable pageable);

  @Query(
      """
      SELECT COUNT(*) FROM lah."Tag"
      WHERE (:name IS NULL OR LOWER("Name") LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:assignable IS NULL OR "CanUserAssign" = :assignable)
      """)
  Mono<Long> countFiltered(@Param("name") String name, @Param("assignable") Boolean assignable);
}
