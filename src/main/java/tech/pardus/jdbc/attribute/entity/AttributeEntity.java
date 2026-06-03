package tech.pardus.jdbc.attribute.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Attribute", schema = "lah")
public class AttributeEntity implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "AttributeId", nullable = false)
  private Integer id;

  @Column(name = "Name", nullable = false, length = 100)
  private String name;

  @Column(name = "Description", length = 255)
  private String description;

  @Column(name = "Type", nullable = false, length = 20)
  private String type;

  @Column(name = "CreateTime")
  private LocalDateTime createTime;

  @Column(name = "ModifyTime")
  private LocalDateTime modifyTime;

  @Column(name = "IsGMP", nullable = false)
  private Boolean isGmp;

  @Column(name = "ShowToAdmin")
  private Boolean showToAdmin;

  @Column(name = "OperantUser", length = 50)
  private String operantUser;

  @Column(name = "ShowToUser")
  private Boolean showToUser;

  @PrePersist
  void onCreate() {
    var now = LocalDateTime.now();
    if (createTime == null) {
      createTime = now;
    }
    modifyTime = now;
  }

  @PreUpdate
  void onUpdate() {
    modifyTime = LocalDateTime.now();
  }
}
