package com.orbit.gateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallbackController.class)
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("GET /fallback/ingest returns 503 and valid fallback payload")
    void testIngestFallbackGet() {
        webTestClient.get().uri("/fallback/ingest")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-ingest")
                .jsonPath("$.status").isEqualTo("UNAVAILABLE")
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    @DisplayName("POST /fallback/ingest returns 503 and valid fallback payload")
    void testIngestFallbackPost() {
        webTestClient.post().uri("/fallback/ingest")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-ingest")
                .jsonPath("$.status").isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("GET /fallback/processor returns 503 and valid fallback payload")
    void testProcessorFallbackGet() {
        webTestClient.get().uri("/fallback/processor")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-processor")
                .jsonPath("$.status").isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("POST /fallback/processor returns 503 and valid fallback payload")
    void testProcessorFallbackPost() {
        webTestClient.post().uri("/fallback/processor")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-processor")
                .jsonPath("$.status").isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("GET /fallback/orchestrator returns 503 and valid fallback payload")
    void testOrchestratorFallbackGet() {
        webTestClient.get().uri("/fallback/orchestrator")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-orchestrator")
                .jsonPath("$.status").isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("POST /fallback/orchestrator returns 503 and valid fallback payload")
    void testOrchestratorFallbackPost() {
        webTestClient.post().uri("/fallback/orchestrator")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-orchestrator")
                .jsonPath("$.status").isEqualTo("UNAVAILABLE");
    }
}
