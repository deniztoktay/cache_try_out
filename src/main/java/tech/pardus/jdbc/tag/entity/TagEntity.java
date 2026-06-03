package tech.pardus.jdbc.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.pardus.jdbc.tag.enums.TagUsageType;

/**
 * Maps to {@code MLMDB_PUURS.lah.Tag}: TagId IDENTITY, Uk_Tags (Name, Type).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Tag", schema = "lah")
public class TagEntity implements Serializable {

  private static final long serialVersionUID = -6361021938142144330L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "TagId", nullable = false)
  private Integer id;

  @Column(name = "Name", nullable = false, length = 50)
  private String name;

  @Column(name = "Type", nullable = false, length = 25)
  private String type;

  @Enumerated(EnumType.STRING)
  @Column(name = "UsageType", length = 6)
  private TagUsageType usageType;

  @Column(name = "CanUserAssign")
  private Boolean canUserAssign;
}
