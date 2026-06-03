package tech.pardus.redis.dto;

import java.util.Map;

public record StreamMessage(String id, Map<String, String> fields) {}
