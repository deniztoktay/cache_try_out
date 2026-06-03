package tech.pardus.r2dbc.reference.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Read model for lah.Reference (audit columns omitted). */
@Table(name = "Reference", schema = "lah")
public record ReferenceRecord(
    @Id @Column("ReferenceId") Integer id,
    @Column("Value") String value,
    @Column("ReferenceTypeId") Integer referenceTypeId,
    @Column("UserId") String userId) {}
