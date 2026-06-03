package tech.pardus.redis.dto;

public record LockHandle(String lockKey, String ownerId) {}
