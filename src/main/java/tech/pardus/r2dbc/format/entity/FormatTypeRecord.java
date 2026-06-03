package tech.pardus.r2dbc.format.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Read model for lah.FormatType (audit columns omitted). */
@Table(name = "FormatType", schema = "lah")
public record FormatTypeRecord(
    @Id @Column("FormatTypeId") Integer id,
    @Column("FormatValue") String formatValue,
    @Column("Description") String description,
    @Column("Type") String type,
    @Column("culture") String culture) {}
