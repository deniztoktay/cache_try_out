package tech.pardus.newdesign.tag.r2dbc.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "Tag", schema = "lah")
public record TagRecord(
    @Id @Column("TagId") Integer id,
    @Column("Name") String name,
    @Column("Type") String type,
    @Column("UsageType") String usageType,
    @Column("CanUserAssign") Boolean canUserAssign) {}
