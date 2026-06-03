package tech.pardus.attribute.model;

import java.time.LocalDateTime;

public record Attribute(
    Integer id,
    String name,
    String description,
    String type,
    LocalDateTime createTime,
    LocalDateTime modifyTime,
    Boolean isGmp,
    Boolean showToAdmin,
    String operantUser,
    Boolean showToUser) {}
