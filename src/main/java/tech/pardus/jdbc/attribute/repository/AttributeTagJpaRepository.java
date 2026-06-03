package tech.pardus.jdbc.attribute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.jdbc.attribute.entity.AttributeTagEntity;
import tech.pardus.jdbc.attribute.entity.compositeid.AttributeTagId;

/**
 * @author TOKTAD <deniz.toktay@pfizer.com>
 * @since Jul 30, 2024
 */
public interface AttributeTagJpaRepository
    extends JpaRepository<AttributeTagEntity, AttributeTagId> {

  boolean existsByAttributeIdAndTagId(Integer attributeId, Integer tagId);

  void deleteByAttributeIdAndTagId(Integer attributeId, Integer tagId);
}
