package com.orbit.processor.engine;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.DeviceTelemetry;
import com.orbit.processor.domain.Severity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ThresholdRuleTest {

    private final ThresholdRule rule = new ThresholdRule();

    @Test
    void shouldDetectCriticalTemperature() {
        DeviceTelemetry telemetry = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("TEMPERATURE")
                .value(90.0)
                .timestamp(System.currentTimeMillis())
                .build();

        Optional<AnomalyEvent> event = rule.evaluate(telemetry);
        assertTrue(event.isPresent());
        assertEquals(Severity.CRITICAL, event.get().getSeverity());
    }

    @Test
    void shouldDetectHighTemperature() {
        DeviceTelemetry telemetry = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("TEMPERATURE")
                .value(75.0)
                .timestamp(System.currentTimeMillis())
                .build();

        Optional<AnomalyEvent> event = rule.evaluate(telemetry);
        assertTrue(event.isPresent());
        assertEquals(Severity.HIGH, event.get().getSeverity());
    }

    @Test
    void shouldNotDetectNormalTemperature() {
        DeviceTelemetry telemetry = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("TEMPERATURE")
                .value(60.0)
                .timestamp(System.currentTimeMillis())
                .build();

        Optional<AnomalyEvent> event = rule.evaluate(telemetry);
        assertFalse(event.isPresent());
    }

    @Test
    void shouldDetectBatteryAnomalies() {
        DeviceTelemetry criticalBattery = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("BATTERY_LEVEL")
                .value(5.0)
                .timestamp(System.currentTimeMillis())
                .build();
        Optional<AnomalyEvent> critEvent = rule.evaluate(criticalBattery);
        assertTrue(critEvent.isPresent());
        assertEquals(Severity.CRITICAL, critEvent.get().getSeverity());

        DeviceTelemetry lowBattery = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("BATTERY_LEVEL")
                .value(15.0)
                .timestamp(System.currentTimeMillis())
                .build();
        Optional<AnomalyEvent> lowEvent = rule.evaluate(lowBattery);
        assertTrue(lowEvent.isPresent());
        assertEquals(Severity.HIGH, lowEvent.get().getSeverity());

        DeviceTelemetry normalBattery = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("BATTERY_LEVEL")
                .value(80.0)
                .timestamp(System.currentTimeMillis())
                .build();
        assertFalse(rule.evaluate(normalBattery).isPresent());
    }

    @Test
    void shouldDetectCpuAnomalies() {
        DeviceTelemetry criticalCpu = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("CPU_USAGE")
                .value(98.0)
                .timestamp(System.currentTimeMillis())
                .build();
        Optional<AnomalyEvent> critEvent = rule.evaluate(criticalCpu);
        assertTrue(critEvent.isPresent());
        assertEquals(Severity.CRITICAL, critEvent.get().getSeverity());

        DeviceTelemetry highCpu = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("CPU_USAGE")
                .value(85.0)
                .timestamp(System.currentTimeMillis())
                .build();
        Optional<AnomalyEvent> highEvent = rule.evaluate(highCpu);
        assertTrue(highEvent.isPresent());
        assertEquals(Severity.HIGH, highEvent.get().getSeverity());
    }

    @Test
    void shouldDetectMemoryAnomalies() {
        DeviceTelemetry criticalMem = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("MEMORY_USAGE")
                .value(95.0)
                .timestamp(System.currentTimeMillis())
                .build();
        Optional<AnomalyEvent> critEvent = rule.evaluate(criticalMem);
        assertTrue(critEvent.isPresent());
        assertEquals(Severity.CRITICAL, critEvent.get().getSeverity());

        DeviceTelemetry highMem = DeviceTelemetry.builder()
                .deviceId("dev-1")
                .metricType("MEMORY_USAGE")
                .value(78.0)
                .timestamp(System.currentTimeMillis())
                .build();
        Optional<AnomalyEvent> highEvent = rule.evaluate(highMem);
        assertTrue(highEvent.isPresent());
        assertEquals(Severity.HIGH, highEvent.get().getSeverity());
    }
}
