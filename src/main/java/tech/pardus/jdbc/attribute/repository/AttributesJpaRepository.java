package tech.pardus.jdbc.attribute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.jdbc.attribute.entity.AttributeEntity;

public interface AttributesJpaRepository extends JpaRepository<AttributeEntity, Integer> {}
