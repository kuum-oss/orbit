package com.orbit.ingest.controller;

import com.orbit.ingest.domain.DeviceTelemetry;
import com.orbit.ingest.service.TelemetryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/telemetry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<DeviceTelemetry> ingestTelemetry(@RequestBody DeviceTelemetry telemetry) {
        return telemetryService.processTelemetry(telemetry);
    }

    @PostMapping("/telemetry/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Flux<DeviceTelemetry> ingestTelemetryBatch(@RequestBody Flux<DeviceTelemetry> telemetryBatch) {
        return telemetryService.processBatch(telemetryBatch);
    }

    @GetMapping(value = "/telemetry/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DeviceTelemetry> streamTelemetry() {
        // Simple mock for streaming
        return Flux.empty();
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}
