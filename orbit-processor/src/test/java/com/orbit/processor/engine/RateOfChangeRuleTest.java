package com.orbit.processor.engine;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.DeviceTelemetry;
import com.orbit.processor.domain.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateOfChangeRuleTest {

    private RateOfChangeRule rule;

    @BeforeEach
    void setUp() {
        rule = new RateOfChangeRule();
        ReflectionTestUtils.setField(rule, "windowSize", 5);
        ReflectionTestUtils.setField(rule, "temperatureChangeThreshold", 15.0);
    }

    @Test
    void shouldDetectSuddenTemperatureSpike() {
        DeviceTelemetry t1 = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("TEMPERATURE")
                .value(20.0)
                .timestamp(1000)
                .build();
                
        DeviceTelemetry t2 = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("TEMPERATURE")
                .value(40.0)
                .timestamp(2000)
                .build();

        Optional<AnomalyEvent> event1 = rule.evaluate(t1);
        assertFalse(event1.isPresent());

        Optional<AnomalyEvent> event2 = rule.evaluate(t2);
        assertTrue(event2.isPresent());
        assertEquals(Severity.HIGH, event2.get().getSeverity());
    }
    
    @Test
    void shouldNotDetectGradualChange() {
        DeviceTelemetry t1 = DeviceTelemetry.builder()
                .deviceId("dev-2")
                .metricType("TEMPERATURE")
                .value(20.0)
                .timestamp(1000)
                .build();
                
        DeviceTelemetry t2 = DeviceTelemetry.builder()
                .deviceId("dev-2")
                .metricType("TEMPERATURE")
                .value(25.0)
                .timestamp(2000)
                .build();

        Optional<AnomalyEvent> event1 = rule.evaluate(t1);
        assertFalse(event1.isPresent());

        Optional<AnomalyEvent> event2 = rule.evaluate(t2);
        assertFalse(event2.isPresent());
    }
}
