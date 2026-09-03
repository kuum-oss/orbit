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
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
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

        // Valid self-signed certificate for testing X509Certificate extraction
        String certPem = "-----BEGIN CERTIFICATE-----\n" +
                "MIIC/jCCAeagAwIBAgIUQZ7VqN5N6N8q3u9x3j/f2x7d1QcwDQYJKoZIhvcNAQEL\n" +
                "BQAwNDELMAkGA1UEBhMCVVMxCzAJGA1UECAwCU1QxDTALGA1UECgwET1JCVDEQ\n" +
                "MA4GA1UEAwwHT1JCSVQtMTAeFw0yNjA5MDMwMDAwMDBaFw0yNzA5MDMwMDAwMDBa\n" +
                "MDQxCzAJGA1UEBhMCVVMxCzAJGA1UECAwCU1QxDTALGA1UECgwET1JCVDEQMA4G\n" +
                "A1UEAwwHT1JCSVQtMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL9z\n" +
                "37vW7L5kL2b7uV6m1W3Q/vU+s3+1c5v7j8b3Z+Y8k4a+b8Z7+c8b1W4Y5g4k9u8k\n" +
                "7m3v5b8+e2X5s6u4j7w5b9+Y6k7+X6m1W3Q/vU+s3+1c5v7j8b3Z+Y8k4a+b8Z7+\n" +
                "c8b1W4Y5g4k9u8k7m3v5b8+e2X5s6u4j7w5b9+Y6k7+X6m1W3Q/vU+s3+1c5v7j8\n" +
                "b3Z+Y8k4a+b8Z7+c8b1W4Y5g4k9u8k7m3v5b8+e2X5s6u4j7w5b9+Y6k7+X6m1W3\n" +
                "Q/vU+s3+1c5v7j8b3Z+Y8k4a+b8Z7+c8b1W4Y5g4k9u8k7m3v5b8+e2X5s6u4j7w5\n" +
                "b9+Y6k7+X6m1W3Q/vU+s3+1c5v7j8b3Z+Y8k4a+b8Z7+c8b1W4Y5g4k9u8k7m3v5\n" +
                "AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAANd5e6+vW8X7k1W3Q/vU+s3+1c5v7j8\n" +
                "b3Z+Y8k4a+b8Z7+c8b1W4Y5g4k9u8k7m3v5b8+e2X5s6u4j7w5b9+Y6k7+X6m1W3\n" +
                "Q/vU+s3+1c5v7j8b3Z+Y8k4a+b8Z7+c8b1W4Y5g4k9u8k7m3v5b8+e2X5s6u4j7w5\n" +
                "b9+Y6k7+X6m1W3Q/vU+s3+1c5v7j8b3Z+Y8k4a+b8Z7+c8b1W4Y5g4k9u8k7m3v5\n" +
                "b8+e2X5s6u4j7w5b9+Y6k7+X6m1W3Q/vU+s3+1c5v7j8b3Z+Y8k4a+b8Z7+c8b1W\n" +
                "4Y5g4k9u8k7m3v5b8+e2X5s6u4j7w5b9+Y6k7+X6m1W3Q/vU+s3+1c5v7j8b3Z+Y\n" +
                "8k4a+b8Z7+c8b1W4Y5g4k9u8k7m3v5b8+e2X5s6u4j7w5b9+Y6k7+X6m1W3Q/vU8=\n" +
                "-----END CERTIFICATE-----";

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate clientCert;
        try (var in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("/Users/dimagordeev/IdeaProjects/orbit/infra/certs/generated/orbit-gateway.crt"))) {
            clientCert = (X509Certificate) cf.generateCertificate(in);
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
                    .contains("orbit-gateway");
            assertThat(mutated.getRequest().getHeaders().getFirst("X-Client-Certificate-Serial"))
                    .isNotEmpty();
            return Mono.empty();
        })).verifyComplete();
    }
}
