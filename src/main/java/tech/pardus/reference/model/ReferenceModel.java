package tech.pardus.reference.model;

import tech.pardus.redis.cache.Identifiable;

public record ReferenceModel(Integer id, String value, Integer referenceTypeId, String userId)
    implements Identifiable<Integer> {

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public String getStringId() {
    return String.valueOf(id);
  }
}
