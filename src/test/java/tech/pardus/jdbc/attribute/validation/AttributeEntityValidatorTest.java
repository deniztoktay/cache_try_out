package tech.pardus.jdbc.attribute.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.jdbc.attribute.entity.AttributeEntity;
import tech.pardus.jdbc.attribute.view.AttributeView;
import tech.pardus.jdbc.attribute.view.AttributeViewService;

class AttributeEntityValidatorTest {

  private AttributeViewService attributeViewService;
  private AttributeEntityValidator validator;

  @BeforeEach
  void setUp() {
    attributeViewService = mock(AttributeViewService.class);
    validator = new AttributeEntityValidator(attributeViewService);
  }

  @Test
  void validateUq_rejectsDuplicateName() {
    var entity = AttributeEntity.builder().name("Color").type("STRING").isGmp(false).build();
    when(attributeViewService.existsByName(eq("Color"), eq(null))).thenReturn(true);

    assertThrows(PRuntimeException.class, () -> validator.validateUq(entity).block());
  }

  @Test
  void validateFk_requiresExistingIdOnUpdate() {
    var entity =
        AttributeEntity.builder().id(5).name("Color").type("STRING").isGmp(false).build();
    when(attributeViewService.findById(5)).thenReturn(Optional.empty());

    assertThrows(PRuntimeException.class, () -> validator.validateFk(entity).block());
  }

  @Test
  void validateFk_passesWhenIdExists() {
    var entity =
        AttributeEntity.builder().id(1).name("Color").type("STRING").isGmp(false).build();
    when(attributeViewService.findById(1))
        .thenReturn(Optional.of(new AttributeView(1, "Color", null, "STRING", false, true, null, true)));

    validator.validateFk(entity).block();
  }
}
