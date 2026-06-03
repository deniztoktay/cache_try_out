package tech.pardus.r2dbc.attribute.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

/** Read model for lah.AttributeTag. */
@Table(name = "AttributeTag", schema = "lah")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class AttributeTagRecord implements Persistable<AttributeTagKey> {

  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
  @Id
  private AttributeTagKey id;

  @Transient private boolean newEntity = true;

  @Override
  public boolean isNew() {
    return newEntity;
  }

  public Integer getAttributeId() {
    return id != null ? id.attributeId() : null;
  }

  public Integer getTagId() {
    return id != null ? id.tagId() : null;
  }
}
