package tech.pardus.attributetag.model;

import java.time.LocalDateTime;

public record AttributeTag(
    Integer attributeId, Integer tagId, String userId, LocalDateTime createTime, LocalDateTime modifyTime) {}
