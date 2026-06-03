package tech.pardus.jdbc.reference.view;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tech.pardus.cache.read.AbstractValidationView;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.reference.model.ReferenceModel;

@Service
public class ReferenceViewService
    extends AbstractValidationView<Integer, ReferenceView, ReferenceModel> {

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  public ReferenceViewService(
      @Qualifier("referenceCacheReadStrategy")
          CacheReadStrategy<Integer, ReferenceModel> referenceCacheReadStrategy) {
    super(referenceCacheReadStrategy, ReferenceView::fromModel, READ_TIMEOUT);
  }

  public Optional<ReferenceView> findById(Integer id) {
    return findByIdForValidation(id);
  }

  /** Uk_reference on ({@code ReferenceTypeId}, {@code Value}). */
  public boolean existsByReferenceTypeIdAndValue(
      Integer referenceTypeId, String value, Integer excludeId) {
    if (value == null) {
      return false;
    }
    return findAllForValidation().stream()
        .filter(row -> excludeId == null || !Objects.equals(row.id(), excludeId))
        .anyMatch(
            row ->
                Objects.equals(referenceTypeId, row.referenceTypeId())
                    && value.equals(row.value()));
  }
}
