package tech.pardus.r2dbc.attribute.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "Attribute", schema = "lah")
public record AttributeRecord(
    @Id @Column("AttributeId") Integer id,
    @Column("Name") String name,
    @Column("Description") String description,
    @Column("Type") String type,
    @Column("CreateTime") LocalDateTime createTime,
    @Column("ModifyTime") LocalDateTime modifyTime,
    @Column("IsGMP") Boolean isGmp,
    @Column("ShowToAdmin") Boolean showToAdmin,
    @Column("OperantUser") String operantUser,
    @Column("ShowToUser") Boolean showToUser) {}
