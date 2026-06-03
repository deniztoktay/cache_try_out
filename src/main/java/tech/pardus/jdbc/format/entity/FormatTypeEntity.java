package tech.pardus.jdbc.format.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "FormatType", schema = "lah")
public class FormatTypeEntity implements Serializable {

  private static final long serialVersionUID = 7103785248744579420L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "FormatTypeId", nullable = false)
  private Integer id;

  @Column(name = "FormatValue", nullable = false)
  private String formatValue;

  @Column(name = "Description")
  private String description;

  @Column(name = "Type", length = 50)
  private String type;

  @Column(name = "culture", length = 25)
  private String culture;

  @Column(name = "CreateTime")
  private Instant createTime;

  @Column(name = "ModifyTime")
  private Instant modifyTime;

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
