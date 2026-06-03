package tech.pardus.jdbc.tag.view;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tech.pardus.cache.read.AbstractValidationView;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.tag.model.TagModel;

@Service
public class TagViewService extends AbstractValidationView<Integer, TagView, TagModel> {

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  public TagViewService(
      @Qualifier("tagCacheReadStrategy") CacheReadStrategy<Integer, TagModel> tagCacheReadStrategy) {
    super(tagCacheReadStrategy, TagView::fromModel, READ_TIMEOUT);
  }

  public Optional<TagView> findById(Integer id) {
    return findByIdForValidation(id);
  }

  public boolean existsByNameAndType(String name, String type, Integer excludeId) {
    if (name == null || type == null) {
      return false;
    }
    return findAllForValidation().stream()
        .filter(tag -> excludeId == null || !Objects.equals(tag.id(), excludeId))
        .anyMatch(tag -> name.equals(tag.name()) && type.equals(tag.type()));
  }
}
