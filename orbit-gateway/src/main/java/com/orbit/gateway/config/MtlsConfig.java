package com.orbit.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * mTLS configuration validation.
 * Validates that SSL bundles for gateway are properly loaded.
 * The actual SSL configuration is done via Spring Boot SSL bundles in application.yml.
 */
@Configuration
@ConditionalOnProperty(name = "orbit.mtls.enabled", havingValue = "true")
public class MtlsConfig {

    private static final Logger log = LoggerFactory.getLogger(MtlsConfig.class);

    private final SslBundles sslBundles;

    public MtlsConfig(SslBundles sslBundles) {
        this.sslBundles = sslBundles;
    }

    @PostConstruct
    public void validateMtlsSetup() {
        try {
            var serverBundle = sslBundles.getBundle("gateway-server");
            log.info("mTLS server bundle loaded successfully: protocol={}",
                    serverBundle.createSslContext().getProtocol());
        } catch (Exception e) {
            log.error("Failed to load mTLS server SSL bundle 'gateway-server'. " +
                    "Ensure certificates are generated via infra/certs/generate-certs.sh", e);
            throw new IllegalStateException("mTLS configuration failed", e);
        }
    }
}
