package tech.pardus.jdbc.format.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.jdbc.format.entity.FormatTypeEntity;
import tech.pardus.jdbc.format.view.FormatTypeView;
import tech.pardus.jdbc.format.view.FormatTypeViewService;

class FormatTypeEntityValidatorTest {

  private FormatTypeViewService viewService;
  private FormatTypeEntityValidator validator;

  @BeforeEach
  void setUp() {
    viewService = mock(FormatTypeViewService.class);
    validator = new FormatTypeEntityValidator(viewService);
  }

  @Test
  void validateUq_rejectsDuplicateValueCulture() {
    var entity = new FormatTypeEntity();
    entity.setFormatValue("fmt");
    entity.setCulture("en");
    when(viewService.existsByFormatValueAndCulture("fmt", "en", null)).thenReturn(true);

    assertThrows(PRuntimeException.class, () -> validator.validateUq(entity).block());
  }

  @Test
  void validateFk_requiresExistingIdOnUpdate() {
    var entity = new FormatTypeEntity();
    entity.setId(9);
    entity.setFormatValue("fmt");
    entity.setCulture("en");
    when(viewService.findById(9)).thenReturn(Optional.empty());

    assertThrows(PRuntimeException.class, () -> validator.validateFk(entity).block());
  }

  @Test
  void validateFk_passesWhenIdExists() {
    var entity = new FormatTypeEntity();
    entity.setId(1);
    entity.setFormatValue("fmt");
    entity.setCulture("en");
    when(viewService.findById(1))
        .thenReturn(Optional.of(new FormatTypeView(1, "fmt", null, null, "en")));

    validator.validateFk(entity).block();
  }
}
