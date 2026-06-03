package tech.pardus.jdbc.attributetag.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pardus.exception.PRuntimeException;
import tech.pardus.jdbc.attribute.entity.AttributeTagEntity;
import tech.pardus.jdbc.attribute.view.AttributeView;
import tech.pardus.jdbc.attribute.view.AttributeViewService;
import tech.pardus.jdbc.attributetag.view.AttributeTagView;
import tech.pardus.jdbc.attributetag.view.AttributeTagViewService;
import tech.pardus.jdbc.tag.view.TagView;
import tech.pardus.jdbc.tag.view.TagViewService;

class AttributeTagEntityValidatorTest {

  private AttributeTagViewService attributeTagViewService;
  private AttributeViewService attributeViewService;
  private TagViewService tagViewService;
  private AttributeTagEntityValidator validator;

  @BeforeEach
  void setUp() {
    attributeTagViewService = mock(AttributeTagViewService.class);
    attributeViewService = mock(AttributeViewService.class);
    tagViewService = mock(TagViewService.class);
    validator =
        new AttributeTagEntityValidator(attributeTagViewService, attributeViewService, tagViewService);
  }

  @Test
  void validateUq_rejectsExistingLink() {
    var entity = AttributeTagEntity.builder().attributeId(1).tagId(2).build();
    when(attributeTagViewService.existsByAttributeIdAndTagId(1, 2)).thenReturn(true);

    assertThrows(PRuntimeException.class, () -> validator.validateUq(entity).block());
  }

  @Test
  void validateFk_requiresAttributeAndTag() {
    var entity = AttributeTagEntity.builder().attributeId(1).tagId(2).build();
    when(attributeViewService.findById(1)).thenReturn(Optional.empty());

    assertThrows(PRuntimeException.class, () -> validator.validateFk(entity).block());
  }

  @Test
  void validateFk_passesWhenParentsExist() {
    var entity = AttributeTagEntity.builder().attributeId(1).tagId(2).build();
    when(attributeViewService.findById(1))
        .thenReturn(Optional.of(new AttributeView(1, "A", null, "T", false, true, null, true)));
    when(tagViewService.findById(2))
        .thenReturn(Optional.of(new TagView(2, "tag", "TYPE", null, false)));

    validator.validateFk(entity).block();
  }
}
