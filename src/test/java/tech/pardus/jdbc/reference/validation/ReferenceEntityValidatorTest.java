package tech.pardus.jdbc.reference.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.jdbc.reference.entity.ReferenceEntity;
import tech.pardus.jdbc.reference.view.ReferenceView;
import tech.pardus.jdbc.reference.view.ReferenceViewService;

class ReferenceEntityValidatorTest {

  private ReferenceViewService viewService;
  private ReferenceEntityValidator validator;

  @BeforeEach
  void setUp() {
    viewService = mock(ReferenceViewService.class);
    validator = new ReferenceEntityValidator(viewService);
  }

  @Test
  void validateUq_rejectsDuplicateTypeValue() {
    var entity =
        ReferenceEntity.builder().value("ref-val").referenceTypeId(2).userId("user1").build();
    when(viewService.existsByReferenceTypeIdAndValue(2, "ref-val", null)).thenReturn(true);

    assertThrows(PRuntimeException.class, () -> validator.validateUq(entity).block());
  }

  @Test
  void validateFk_requiresExistingIdOnUpdate() {
    var entity =
        ReferenceEntity.builder()
            .id(9)
            .value("ref-val")
            .referenceTypeId(2)
            .userId("user1")
            .build();
    when(viewService.findById(9)).thenReturn(Optional.empty());

    assertThrows(PRuntimeException.class, () -> validator.validateFk(entity).block());
  }

  @Test
  void validateFk_passesWhenIdExists() {
    var entity =
        ReferenceEntity.builder()
            .id(1)
            .value("ref-val")
            .referenceTypeId(2)
            .userId("user1")
            .build();
    when(viewService.findById(1))
        .thenReturn(Optional.of(new ReferenceView(1, "ref-val", 2, "user1")));

    validator.validateFk(entity).block();
  }
}
