package tech.pardus.jdbc.tag.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.pardus.jdbc.tag.entity.TagEntity;

/**
 * @author TOKTAD <deniz.toktay@pfizer.com>
 * @since Jul 30, 2024
 */
public interface TagsJpaRepository extends JpaRepository<TagEntity, Integer> {}
