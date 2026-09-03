package com.orbit.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;

/**
 * Filter that extracts mTLS client certificate information
 * and adds it as request headers for downstream services.
 * When mTLS is enabled, this validates that the client presented
 * a valid certificate signed by the Orbit CA.
 */
@Component
@ConditionalOnProperty(name = "orbit.mtls.enabled", havingValue = "true")
public class MtlsAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(MtlsAuthenticationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        SSLSession sslSession = (SSLSession) exchange.getAttributes().get("javax.net.ssl.SSLSession");

        if (sslSession != null) {
            try {
                var peerCerts = sslSession.getPeerCertificates();
                if (peerCerts.length > 0 && peerCerts[0] instanceof X509Certificate clientCert) {
                    String cn = clientCert.getSubjectX500Principal().getName();
                    String serial = clientCert.getSerialNumber().toString(16);

                    log.debug("mTLS client authenticated: CN={}, Serial={}", cn, serial);

                    var mutatedRequest = exchange.getRequest().mutate()
                            .header("X-Client-Certificate-CN", cn)
                            .header("X-Client-Certificate-Serial", serial)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }
            } catch (Exception e) {
                log.warn("Failed to extract client certificate from SSL session", e);
            }
        }

        // If mTLS is enabled but no cert — check if path is excluded
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator") || path.startsWith("/fallback")) {
            return chain.filter(exchange);
        }

        log.warn("No client certificate presented for mTLS-protected path: {}", path);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
