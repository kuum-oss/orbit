package com.orbit.ingest.controller;

import com.orbit.ingest.domain.DeviceTelemetry;
import com.orbit.ingest.service.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(TelemetryController.class)
class TelemetryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TelemetryService telemetryService;

    @Test
    void testIngestTelemetry() {
        DeviceTelemetry telemetry = new DeviceTelemetry();
        telemetry.setDeviceId("device-1");
        telemetry.setMetricType("temperature");
        telemetry.setValue(25.5);

        DeviceTelemetry saved = new DeviceTelemetry();
        saved.setId(UUID.randomUUID());
        saved.setDeviceId("device-1");
        saved.setMetricType("temperature");
        saved.setValue(25.5);
        saved.setReceivedAt(Instant.now());

        when(telemetryService.processTelemetry(any(DeviceTelemetry.class))).thenReturn(Mono.just(saved));

        webTestClient.post()
                .uri("/api/v1/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(telemetry)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.deviceId").isEqualTo("device-1");
    }

    @Test
    void testIngestTelemetryBatch() {
        DeviceTelemetry t1 = new DeviceTelemetry();
        t1.setDeviceId("device-1");

        DeviceTelemetry t2 = new DeviceTelemetry();
        t2.setDeviceId("device-2");

        when(telemetryService.processBatch(any())).thenReturn(Flux.just(t1, t2));

        webTestClient.post()
                .uri("/api/v1/telemetry/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Arrays.asList(t1, t2))
                .exchange()
                .expectStatus().isAccepted()
                .expectBodyList(DeviceTelemetry.class)
                .hasSize(2);
    }
}
