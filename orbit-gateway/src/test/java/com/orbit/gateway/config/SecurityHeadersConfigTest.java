package com.orbit.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityHeadersConfigTest {

    @Test
    @DisplayName("Adds standard security headers to every HTTP response")
    void testSecurityHeaders() {
        SecurityHeadersConfig config = new SecurityHeadersConfig();
        WebFilter filter = config.securityHeadersFilter();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/tickets").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Content-Type-Options"))
                .isEqualTo("nosniff");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Frame-Options"))
                .isEqualTo("DENY");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-XSS-Protection"))
                .isEqualTo("1; mode=block");
        assertThat(exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }
}
