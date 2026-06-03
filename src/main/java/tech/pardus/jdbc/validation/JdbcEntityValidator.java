package tech.pardus.jdbc.validation;

import reactor.core.publisher.Mono;

/** Validates an entity before persistence (field rules, FK existence, unique constraints). */
public interface JdbcEntityValidator<T> {

  Mono<Void> validateForFields(T entity);

  Mono<Void> validateUq(T entity);

  Mono<Void> validateFk(T entity);

  default Mono<Void> validateForInsert(T entity) {
    return validateForFields(entity).then(validateFk(entity)).then(validateUq(entity));
  }

  default Mono<Void> validateForUpdate(T entity) {
    return validateForFields(entity).then(validateFk(entity)).then(validateUq(entity));
  }
}
