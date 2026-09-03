package com.orbit.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayRoutesConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void allRoutesAreRegistered() {
        StepVerifier.create(routeLocator.getRoutes().map(r -> r.getId()))
                .expectNextMatches(id -> id.equals("ingest-telemetry"))
                .expectNextMatches(id -> id.equals("ingest-health"))
                .expectNextMatches(id -> id.equals("processor-anomalies"))
                .expectNextMatches(id -> id.equals("orchestrator-tickets"))
                .expectNextMatches(id -> id.equals("ingest-actuator"))
                .expectNextMatches(id -> id.equals("processor-actuator"))
                .expectNextMatches(id -> id.equals("orchestrator-actuator"))
                .verifyComplete();
    }

    @Test
    void gatewayInfoEndpointWorks() {
        webTestClient.get().uri("/gateway/info")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-gateway")
                .jsonPath("$.mtls").isEqualTo(false)
                .jsonPath("$.routes").isArray();
    }

    @Test
    void actuatorHealthEndpointWorks() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void fallbackEndpointsReturnServiceUnavailable() {
        webTestClient.get().uri("/fallback/ingest")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-ingest")
                .jsonPath("$.status").isEqualTo("UNAVAILABLE");

        webTestClient.get().uri("/fallback/processor")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-processor");

        webTestClient.get().uri("/fallback/orchestrator")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-orchestrator");
    }
}
