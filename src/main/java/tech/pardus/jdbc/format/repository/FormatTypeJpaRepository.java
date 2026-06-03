package tech.pardus.jdbc.format.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.jdbc.format.entity.FormatTypeEntity;

/**
 * @author TOKTAD <deniz.toktay@pfizer.com>
 * @since Apr 5, 2024
 */
public interface FormatTypeJpaRepository extends JpaRepository<FormatTypeEntity, Integer> {

}
