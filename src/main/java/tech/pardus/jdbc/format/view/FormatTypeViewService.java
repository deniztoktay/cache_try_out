package tech.pardus.jdbc.format.view;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tech.pardus.cache.read.AbstractValidationView;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.format.model.FormatTypeModel;

@Service
public class FormatTypeViewService extends AbstractValidationView<Integer, FormatTypeView, FormatTypeModel> {

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  public FormatTypeViewService(
      @Qualifier("formatTypeCacheReadStrategy")
          CacheReadStrategy<Integer, FormatTypeModel> formatTypeCacheReadStrategy) {
    super(formatTypeCacheReadStrategy, FormatTypeView::fromModel, READ_TIMEOUT);
  }

  public Optional<FormatTypeView> findById(Integer id) {
    return findByIdForValidation(id);
  }

  /** Uk_FormatType on ({@code FormatValue}, {@code culture}). */
  public boolean existsByFormatValueAndCulture(
      String formatValue, String culture, Integer excludeId) {
    if (formatValue == null) {
      return false;
    }
    return findAllForValidation().stream()
        .filter(row -> excludeId == null || !Objects.equals(row.id(), excludeId))
        .anyMatch(
            row ->
                formatValue.equals(row.formatValue())
                    && Objects.equals(normalizeCulture(culture), normalizeCulture(row.culture())));
  }

  private static String normalizeCulture(String culture) {
    return culture == null ? "" : culture;
  }
}
