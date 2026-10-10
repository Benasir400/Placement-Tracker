package com.placetrack.controller;

import java.time.Instant;
import java.util.Map;

import com.placetrack.dto.HealthResponse;
import com.placetrack.dto.PlatformSummaryResponse;
import com.placetrack.service.PlatformSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final PlatformSummaryService platformSummaryService;

    public PublicController(PlatformSummaryService platformSummaryService) {
        this.platformSummaryService = platformSummaryService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(
            "UP",
            "PlaceTrack",
            "Phase 1 foundation",
            Instant.now(),
            Map.of(
                "backend", "Spring Boot API ready",
                "database", "Flyway schema validation enabled",
                "security", "Public health endpoint open; protected APIs require auth"
            )
        );
    }

    @GetMapping("/summary")
    public PlatformSummaryResponse summary() {
        return platformSummaryService.getSummary();
    }
}
