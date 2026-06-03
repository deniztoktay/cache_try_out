package tech.pardus.jdbc.reference.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.jdbc.reference.entity.ReferenceEntity;

/**
 * @author TOKTAD <deniz.toktay@pfizer.com>
 * @since Apr 5, 2024
 */
public interface ReferenceJpaRepository extends JpaRepository<ReferenceEntity, Integer> {
}
