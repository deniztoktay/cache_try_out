package tech.pardus.newdesign.referencetype.r2dbc.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Read model for lah.ReferenceType (audit columns omitted). */
@Table(name = "ReferenceType", schema = "lah")
public record ReferenceTypeRecord(
    @Id @Column("ReferenceTypeId") Integer id,
    @Column("Name") String name,
    @Column("ShowToUI") Boolean showToUi,
    @Column("Priority") Integer priority) {}
