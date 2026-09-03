package com.orbit.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Circuit breaker fallback controller.
 * Provides graceful degradation when downstream services are unavailable.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping(value = "/ingest", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> ingestFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", "orbit-ingest",
                        "status", "UNAVAILABLE",
                        "message", "Telemetry ingestion service is temporarily unavailable. Please retry later.",
                        "timestamp", Instant.now().toString()
                ));
    }

    @RequestMapping(value = "/processor", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> processorFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", "orbit-processor",
                        "status", "UNAVAILABLE",
                        "message", "Anomaly processor service is temporarily unavailable. Please retry later.",
                        "timestamp", Instant.now().toString()
                ));
    }

    @RequestMapping(value = "/orchestrator", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> orchestratorFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", "orbit-orchestrator",
                        "status", "UNAVAILABLE",
                        "message", "Orchestration service is temporarily unavailable. Please retry later.",
                        "timestamp", Instant.now().toString()
                ));
    }
}
