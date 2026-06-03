package tech.pardus.r2dbc.attribute.entity;

import org.springframework.data.relational.core.mapping.Column;

/** Composite primary key for lah.AttributeTag. */
public record AttributeTagKey(
    @Column("AttributeId") Integer attributeId, @Column("TagId") Integer tagId) {}
