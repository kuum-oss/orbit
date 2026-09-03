package com.orbit.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Exposes gateway metadata and route info.
 */
@RestController
@RequestMapping("/gateway")
public class GatewayInfoController {

    private final RouteLocator routeLocator;
    private final Instant startedAt = Instant.now();

    @Value("${orbit.mtls.enabled:false}")
    private boolean mtlsEnabled;

    public GatewayInfoController(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> info() {
        return routeLocator.getRoutes()
                .map(route -> route.getId())
                .collectList()
                .map(routes -> ResponseEntity.ok(Map.of(
                        "service", "orbit-gateway",
                        "mtls", mtlsEnabled,
                        "startedAt", startedAt.toString(),
                        "routes", routes
                )));
    }
}
