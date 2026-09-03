package com.orbit.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrbitGatewayApplicationTest {

    @Test
    void contextLoads() {
        // Verifies the application context loads successfully
    }
}
