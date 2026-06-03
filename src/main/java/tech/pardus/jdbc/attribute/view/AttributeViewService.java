package tech.pardus.jdbc.attribute.view;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tech.pardus.attribute.model.AttributeModel;
import tech.pardus.cache.read.AbstractValidationView;
import tech.pardus.cache.read.CacheReadStrategy;

@Service
public class AttributeViewService extends AbstractValidationView<Integer, AttributeView, AttributeModel> {

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  public AttributeViewService(
      @Qualifier("attributeCacheReadStrategy")
          CacheReadStrategy<Integer, AttributeModel> attributeCacheReadStrategy) {
    super(attributeCacheReadStrategy, AttributeView::fromModel, READ_TIMEOUT);
  }

  public Optional<AttributeView> findById(Integer id) {
    return findByIdForValidation(id);
  }

  /** Uk_attribute on {@code Name}. */
  public boolean existsByName(String name, Integer excludeId) {
    if (name == null) {
      return false;
    }
    return findAllForValidation().stream()
        .filter(attr -> excludeId == null || !Objects.equals(attr.id(), excludeId))
        .anyMatch(attr -> name.equals(attr.name()));
  }
}
