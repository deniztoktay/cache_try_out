package tech.pardus.newdesign.attributetag.jdbc.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class AttributeTagId implements Serializable {

  private static final long serialVersionUID = -7384413092134166210L;

  private Integer attributeId;
  private Integer tagId;
}
