package tech.pardus.newdesign.attribute.jdbc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.newdesign.attribute.jdbc.entity.AttributeEntity;

public interface AttributesJpaRepository extends JpaRepository<AttributeEntity, Integer> {}
