package tech.pardus.format.model;

import java.time.LocalDateTime;

public record FormatType(
    Integer id,
    String formatValue,
    String description,
    String type,
    String culture,
    LocalDateTime createTime,
    LocalDateTime modifyTime) {}
