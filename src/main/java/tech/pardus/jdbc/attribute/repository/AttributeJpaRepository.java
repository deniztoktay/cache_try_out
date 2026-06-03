package tech.pardus.jdbc.attribute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.jdbc.attribute.entity.AttributeEntity;

/**
 * @author TOKTAD <deniz.toktay@pfizer.com>
 * @since Apr 1, 2024
 */
public interface AttributeJpaRepository extends JpaRepository<AttributeEntity, Integer> {

  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Integer id);
}
