package tech.pardus.newdesign.tag.jdbc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.newdesign.tag.jdbc.entity.TagEntity;

/**
 * @author TOKTAD <deniz.toktay@pfizer.com>
 * @since Jul 30, 2024
 */
public interface TagsJpaRepository extends JpaRepository<TagEntity, Integer> {}
