package com.orbit.gateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.net.URI;

import static org.mockito.Mockito.when;

@WebFluxTest(GatewayInfoController.class)
class GatewayInfoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RouteLocator routeLocator;

    @Test
    @DisplayName("GET /gateway/info returns gateway metadata and routes list")
    void testGetInfo() {
        Route route1 = Route.async()
                .id("ingest-telemetry")
                .uri(URI.create("http://localhost:8081"))
                .predicate(exchange -> true)
                .build();

        Route route2 = Route.async()
                .id("processor-anomalies")
                .uri(URI.create("http://localhost:8082"))
                .predicate(exchange -> true)
                .build();

        when(routeLocator.getRoutes()).thenReturn(Flux.just(route1, route2));

        webTestClient.get().uri("/gateway/info")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.service").isEqualTo("orbit-gateway")
                .jsonPath("$.mtls").isEqualTo(false)
                .jsonPath("$.startedAt").isNotEmpty()
                .jsonPath("$.routes").isArray()
                .jsonPath("$.routes[0]").isEqualTo("ingest-telemetry")
                .jsonPath("$.routes[1]").isEqualTo("processor-anomalies");
    }
}
