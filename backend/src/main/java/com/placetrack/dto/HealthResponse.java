package com.placetrack.dto;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
    String status,
    String application,
    String phase,
    Instant timestamp,
    Map<String, String> modules
) {
}
