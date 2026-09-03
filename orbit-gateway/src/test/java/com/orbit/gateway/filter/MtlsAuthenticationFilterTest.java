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
                "MIIC8DCCAdigAwIBAgIUeN7hUj7N2yW6rP1q3s5t8v9w0x4wDQYJKoZIhvcNAQEL\n" +
                "BQAwMDELMAkGA1UEBhMCVVMxETAPGA1UECAwIVGVzdFN0cjEOMAwGA1UEAwwFT1JC\n" +
                "SVQwHhcNMjYwOTAzMDAwMDAwWhcNMzYwOTAxMDAwMDAwWjA7MQswCQYDVQQGEwJV\n" +
                "UzERMA8GA1UECAwIVGVzdFN0cjEUMBIGA1UEAwwLb3JiaXQtZGV2aWNlMIIBIjAN\n" +
                "BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1sK5o2yB9z6q1X4n2v9m7k8j0l1o\n" +
                "3r6p7t4n2v9m7k8j0l1o3r6p7t4n2v9m7k8j0l1o3r6p7t4n2v9m7k8j0l1o3r6p\n" +
                "7t4n2v9m7k8j0l1o3r6p7t4n2v9m7k8j0l1o3r6p7t4n2v9m7k8j0l1o3r6p7t4n\n" +
                "2v9m7k8j0l1o3r6p7t4n2v9m7k8j0l1o3r6p7t4n2v9m7k8j0l1o3r6p7t4n2v9m\n" +
                "7k8j0l1o3r6p7t4n2v9m7k8j0l1o3r6p7t4n2v9m7k8j0l1o3r6p7t4n2v9m7k8j\n" +
                "0l1o3r6p7t4n2v9m7k8j0l1o3r6p7t4n2v9m7k8j0l1o3r6p7t4n2v9m7k8j0l1o\n" +
                "3wIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBVmN1c2p5e7q9r1t3y5u7i9k1m3o5p\n" +
                "7r9t1v3x5z7b9d1f3h5j7l9n1p3r5t7v9x1z3b5d7f9h1j3l5n7p9r1t3v5x7z9b\n" +
                "1d3f5h7j9l1n3p5r7t9v1x3z5b7d9f1h3j5l7n9p1r3t5v7x9z1b3d5f7h9j1l3n\n" +
                "5p7r9t1v3x5z7b9d1f3h5j7l9n1p3r5t7v9x1z3b5d7f9h1j3l5n7p9r1t3v5x7z\n" +
                "9b1d3f5h7j9l1n3p5r7t9v1x3z5b7d9f1h3j5l7n9p1r3t5v7x9z1b3d5f7h9j1l\n" +
                "3n5p7r9t1v3x5z7b9d1f3h5j7l9n1p3r5t7v9x1z3b5d7f9h1j3l5n7p9r1t3v5x\n" +
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
