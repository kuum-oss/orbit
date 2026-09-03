package com.orbit.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route definitions for the gateway.
 * These complement the YAML-based routes and add filters like
 * circuit breakers, rate limiting, and path rewriting.
 */
@Configuration
public class GatewayRoutesConfig {

    @Value("${orbit.routes.ingest-uri}")
    private String ingestUri;

    @Value("${orbit.routes.processor-uri}")
    private String processorUri;

    @Value("${orbit.routes.orchestrator-uri}")
    private String orchestratorUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Telemetry ingestion — orbit-ingest
                .route("ingest-telemetry", r -> r
                        .path("/api/v1/telemetry/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("ingestCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/ingest"))
                                .addRequestHeader("X-Gateway-Source", "orbit-gateway")
                                .addRequestHeader("X-Forwarded-Prefix", "/api/v1/telemetry"))
                        .uri(ingestUri))

                // Health check for ingest
                .route("ingest-health", r -> r
                        .path("/api/v1/health")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway-Source", "orbit-gateway"))
                        .uri(ingestUri))

                // Anomaly detection — orbit-processor
                .route("processor-anomalies", r -> r
                        .path("/api/v1/anomalies/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("processorCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/processor"))
                                .addRequestHeader("X-Gateway-Source", "orbit-gateway"))
                        .uri(processorUri))

                // Maintenance tickets — orbit-orchestrator
                .route("orchestrator-tickets", r -> r
                        .path("/api/v1/tickets/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("orchestratorCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/orchestrator"))
                                .addRequestHeader("X-Gateway-Source", "orbit-gateway"))
                        .uri(orchestratorUri))

                // Actuator routes for backend services (admin access)
                .route("ingest-actuator", r -> r
                        .path("/admin/ingest/actuator/**")
                        .filters(f -> f
                                .rewritePath("/admin/ingest/actuator/(?<segment>.*)", "/actuator/${segment}")
                                .addRequestHeader("X-Gateway-Source", "orbit-gateway"))
                        .uri(ingestUri))

                .route("processor-actuator", r -> r
                        .path("/admin/processor/actuator/**")
                        .filters(f -> f
                                .rewritePath("/admin/processor/actuator/(?<segment>.*)", "/actuator/${segment}")
                                .addRequestHeader("X-Gateway-Source", "orbit-gateway"))
                        .uri(processorUri))

                .route("orchestrator-actuator", r -> r
                        .path("/admin/orchestrator/actuator/**")
                        .filters(f -> f
                                .rewritePath("/admin/orchestrator/actuator/(?<segment>.*)", "/actuator/${segment}")
                                .addRequestHeader("X-Gateway-Source", "orbit-gateway"))
                        .uri(orchestratorUri))

                .build();
    }
}
