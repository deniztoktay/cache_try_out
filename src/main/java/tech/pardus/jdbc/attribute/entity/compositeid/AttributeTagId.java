package tech.pardus.jdbc.attribute.entity.compositeid;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author TOKTAD <deniz.toktay@pfizer.com>
 * @since Jul 30, 2024
 */
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
