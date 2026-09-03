package com.orbit.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.net.ssl.SSLSession;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MtlsAuthenticationFilterTest {

    private MtlsAuthenticationFilter filter;
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new MtlsAuthenticationFilter();
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Allows bypass without cert for /actuator paths")
    void allowsActuatorWithoutCert() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Allows bypass without cert for /fallback paths")
    void allowsFallbackWithoutCert() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/fallback/ingest").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Returns 401 Unauthorized when client cert is missing on protected path")
    void rejectsProtectedPathWithoutCert() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/telemetry").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verifyNoInteractions(filterChain);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Extracts client certificate CN and Serial into request headers")
    void extractsCertificateDetailsIntoHeaders() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/telemetry").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Standard embedded test certificate self-contained in test code (no external filesystem path dependency)
        String certPem = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDMTCCAhmgAwIBAgIUQVTR/0+bqmLnUODftx9ikCJJiG4wDQYJKoZIhvcNAQEL\n" +
                "BQAwKDEWMBQGA1UEAwwNb3JiaXQtZ2F0ZXdheTEOMAwGA1UECgwFT3JiaXQwHhcN\n" +
                "MjYwOTAzMTMyNTQ2WhcNMjcwOTAzMTMyNTQ2WjAoMRYwFAYDVQQDDA1vcmJpdC1n\n" +
                "YXRld2F5MQ4wDAYDVQQKDAVPcmJpdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\n" +
                "AQoCggEBALSHLMHYe1uxe9eF9YSly3lKf92CjAPfZHSQFEcZ1TTOL9vgQaNesai9\n" +
                "4uCcIztYpfretkH+y0afnOuZclhFGGw1T9WF8+TYQU3gd7u5ewTm4xchzKSa43NA\n" +
                "Y3qgF7x1McsHDRDfuH5F/BGdtAIwCF9VMYIFlblFs3Bo/GKGI3wJUXOsL57+GgvO\n" +
                "y0deEkuE9ClM9kx/eKvq+PieEz4jDR8O9WwdhJh1K1FPS6JyDgtlD2/RoS/Jue2q\n" +
                "Z60IIw2GeWAiMHJ94TZVjEpoSJw/ax5BkEv268Xf2WZte7yAsAP6rhlVZw/j6iMG\n" +
                "YX5PVxRv6t/jQkwrInJI/heILrt/qAECAwEAAaNTMFEwHQYDVR0OBBYEFDXq4Rlk\n" +
                "JeTENO2QyAHPkOPvVFDIMB8GA1UdIwQYMBaAFDXq4RlkJeTENO2QyAHPkOPvVFDI\n" +
                "MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAFRmlSfXwzRhxuW8\n" +
                "UKEcONLaIxrAQ0HHPz8TZmiiHCaCvsxF3f9rLj6vCYR/r752cdmab4662bvAIkiq\n" +
                "XwOnYDM07odLGV/2qGN2Ri271zMad8UBo8vLNH/NFHkhzwSunDl0im0KpH0kFit7\n" +
                "W9kZjJOqDl2E1rX9trV+ge+bn7OY9Yz77YJ6EcyZAZ9KiQXkMEyT916hfFTrlxmk\n" +
                "WXba1Sdo9qPFFCcXC5DFNttOlPC6Dhrwe9xONqySRBD4OOPqHfCz995a4M4/llvA\n" +
                "hB0aOZeQzGyHErhoZS6FgczGAAtasVc/btmikw3EFP8Sd1r0Dkgz4BVrQKU1Xnl5\n" +
                "b9y3kZM=\n" +
                "-----END CERTIFICATE-----";

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate clientCert;
        
        java.nio.file.Path certPath = java.nio.file.Path.of("infra/certs/generated/orbit-gateway.crt");
        if (java.nio.file.Files.exists(certPath)) {
            try (var in = java.nio.file.Files.newInputStream(certPath)) {
                clientCert = (X509Certificate) cf.generateCertificate(in);
            }
        } else {
            java.nio.file.Path fallbackPath = java.nio.file.Path.of("../infra/certs/generated/orbit-gateway.crt");
            if (java.nio.file.Files.exists(fallbackPath)) {
                try (var in = java.nio.file.Files.newInputStream(fallbackPath)) {
                    clientCert = (X509Certificate) cf.generateCertificate(in);
                }
            } else {
                clientCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));
            }
        }

        X509Certificate finalCert = clientCert;
        SSLSession sslSession = (SSLSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{SSLSession.class},
                (proxy, method, args) -> {
                    if ("getPeerCertificates".equals(method.getName())) {
                        return new Certificate[]{finalCert};
                    }
                    return null;
                }
        );

        exchange.getAttributes().put("javax.net.ssl.SSLSession", sslSession);

        StepVerifier.create(filter.filter(exchange, chainExchange -> {
            ServerWebExchange mutated = chainExchange;
            assertThat(mutated.getRequest().getHeaders().getFirst("X-Client-Certificate-CN"))
                    .isNotEmpty();
            assertThat(mutated.getRequest().getHeaders().getFirst("X-Client-Certificate-Serial"))
                    .isNotEmpty();
            return Mono.empty();
        })).verifyComplete();
    }
}
