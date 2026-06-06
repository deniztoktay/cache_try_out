package tech.pardus.newdesign.referencetype.jdbc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

@Entity
@Table(name = "ReferenceType", schema = "lah")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ReferenceTypeEntity implements Serializable {

  private static final long serialVersionUID = -3467107137087825331L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ReferenceTypeId", nullable = false)
  private Integer id;

  @Column(name = "Name", nullable = false, length = 100)
  private String name;

  @Column(name = "CreateTime")
  private Instant createTime;

  @Column(name = "ModifyTime")
  private Instant modifyTime;

  @Column(name = "ShowToUI", nullable = false)
  private Boolean isShowToUI;

  @Column(name = "Priority")
  private Integer priority;

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
