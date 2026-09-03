package com.orbit.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Global filter that logs incoming requests and outgoing responses
 * with timing information for observability.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = Instant.now().toEpochMilli();
        exchange.getAttributes().put(START_TIME_ATTR, startTime);

        String clientIp = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        log.info(">>> Gateway Request: {} {} from {}",
                request.getMethod(), request.getURI().getPath(), clientIp);

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    Long start = exchange.getAttribute(START_TIME_ATTR);
                    long duration = start != null ? Instant.now().toEpochMilli() - start : -1;
                    log.info("<<< Gateway Response: {} {} -> {} ({}ms)",
                            request.getMethod(), request.getURI().getPath(),
                            exchange.getResponse().getStatusCode(), duration);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
