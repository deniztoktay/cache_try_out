package tech.pardus.tag.model;

/** Domain model exposed by the tag service and REST API. */
public record Tag(
    Integer id, String name, String type, String usageType, Boolean canUserAssign) {}
