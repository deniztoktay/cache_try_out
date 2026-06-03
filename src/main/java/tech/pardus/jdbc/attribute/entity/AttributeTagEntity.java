package tech.pardus.jdbc.attribute.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.pardus.jdbc.attribute.entity.compositeid.AttributeTagId;
import tech.pardus.jdbc.tag.entity.TagEntity;

/**
 * @author TOKTAD <deniz.toktay@pfizer.com>
 * @since Jul 30, 2024
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "AttributeTag", schema = "lah")
@IdClass(AttributeTagId.class)
public class AttributeTagEntity implements Serializable {

  private static final long serialVersionUID = 1017868674359586641L;

  @Id
  @Column(name = "AttributeId", nullable = false)
  private Integer attributeId;

  @Id
  @Column(name = "TagId", nullable = false)
  private Integer tagId;

  @Column(name = "UserId")
  private String userId;

  @Column(name = "CreateTime")
  private Instant createTime;

  @Column(name = "ModifyTime")
  private Instant modifyTime;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AttributeId", insertable = false, updatable = false)
  private AttributeEntity attribute;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TagId", insertable = false, updatable = false)
  private TagEntity tag;

  @PrePersist
  void onCreate() {
    var now = Instant.now();
    if (createTime == null) {
      createTime = now;
    }
    modifyTime = now;
  }

  @PreUpdate
  void onUpdate() {
    modifyTime = Instant.now();
  }
}
