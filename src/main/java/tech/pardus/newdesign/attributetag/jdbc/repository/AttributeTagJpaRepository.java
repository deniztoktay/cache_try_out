package tech.pardus.newdesign.attributetag.jdbc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.newdesign.attributetag.jdbc.entity.AttributeTagEntity;
import tech.pardus.newdesign.attributetag.jdbc.entity.AttributeTagId;

public interface AttributeTagJpaRepository extends JpaRepository<AttributeTagEntity, AttributeTagId> {

  boolean existsByAttributeIdAndTagId(Integer attributeId, Integer tagId);

  void deleteByAttributeIdAndTagId(Integer attributeId, Integer tagId);
}
