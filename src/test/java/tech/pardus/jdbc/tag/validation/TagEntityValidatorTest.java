package tech.pardus.jdbc.tag.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.jdbc.tag.entity.TagEntity;
import tech.pardus.jdbc.tag.enums.TagUsageType;
import tech.pardus.jdbc.tag.view.TagView;
import tech.pardus.jdbc.tag.view.TagViewService;

class TagEntityValidatorTest {

  private TagViewService tagViewService;
  private TagEntityValidator validator;

  @BeforeEach
  void setUp() {
    tagViewService = mock(TagViewService.class);
    validator = new TagEntityValidator(tagViewService);
  }

  @Test
  void validateUq_rejectsDuplicateFromViewService() {
    var entity = TagEntity.builder().name("A").type("T1").build();
    when(tagViewService.existsByNameAndType(eq("A"), eq("T1"), eq(null))).thenReturn(true);

    assertThrows(
        PRuntimeException.class, () -> validator.validateUq(entity).block());
  }

  @Test
  void validateFk_requiresExistingIdOnUpdate() {
    var entity = TagEntity.builder().id(99).name("A").type("T1").usageType(TagUsageType.SYSTEM).build();
    when(tagViewService.findById(99)).thenReturn(Optional.empty());

    assertThrows(
        PRuntimeException.class, () -> validator.validateFk(entity).block());
  }

  @Test
  void validateFk_passesWhenIdExists() {
    var entity = TagEntity.builder().id(1).name("A").type("T1").usageType(TagUsageType.SYSTEM).build();
    when(tagViewService.findById(1)).thenReturn(Optional.of(new TagView(1, "A", "T1", TagUsageType.SYSTEM, false)));

    validator.validateFk(entity).block();
  }

  @Test
  void validateForFields_rejectsLongName() {
    var entity = TagEntity.builder().name("x".repeat(51)).type("T1").build();

    assertThrows(
        PRuntimeException.class, () -> validator.validateForFields(entity).block());
  }
}
