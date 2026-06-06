package tech.pardus.newdesign.referencetype.jdbc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.newdesign.referencetype.jdbc.entity.ReferenceTypeEntity;

public interface ReferenceTypeJpaRepository extends JpaRepository<ReferenceTypeEntity, Integer> {}
