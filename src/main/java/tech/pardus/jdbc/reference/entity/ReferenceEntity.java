package tech.pardus.jdbc.reference.entity;

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
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "Reference", schema = "lah")
public class ReferenceEntity implements Serializable {

  private static final long serialVersionUID = -2405578523073835599L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ReferenceId", nullable = false)
  private Integer id;

  @Nationalized
  @Column(name = "Value", nullable = false, length = 255)
  private String value;

  @Column(name = "CreateTime")
  private Instant createTime;

  @Column(name = "ModifyTime")
  private Instant modifyTime;

  @Column(name = "ReferenceTypeId")
  private Integer referenceTypeId;

  @Column(name = "UserId", nullable = false, length = 50)
  private String userId;

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
