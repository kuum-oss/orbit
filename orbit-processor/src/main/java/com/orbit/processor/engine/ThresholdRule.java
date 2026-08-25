package com.orbit.processor.engine;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.DeviceTelemetry;
import com.orbit.processor.domain.Severity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class ThresholdRule implements AnomalyRule {

    @Override
    public Optional<AnomalyEvent> evaluate(DeviceTelemetry telemetry) {
        String metricType = telemetry.getMetricType();
        double value = telemetry.getValue();
        Severity severity = null;
        String description = null;

        if ("TEMPERATURE".equalsIgnoreCase(metricType)) {
            if (value > 85) {
                severity = Severity.CRITICAL;
                description = "Temperature critically high: " + value;
            } else if (value > 70) {
                severity = Severity.HIGH;
                description = "Temperature high: " + value;
            }
        } else if ("BATTERY_LEVEL".equalsIgnoreCase(metricType)) {
            if (value < 10) {
                severity = Severity.CRITICAL;
                description = "Battery critically low: " + value;
            } else if (value < 20) {
                severity = Severity.HIGH;
                description = "Battery low: " + value;
            }
        } else if ("CPU_USAGE".equalsIgnoreCase(metricType)) {
            if (value > 95) {
                severity = Severity.CRITICAL;
                description = "CPU usage critically high: " + value;
            } else if (value > 80) {
                severity = Severity.HIGH;
                description = "CPU usage high: " + value;
            }
        } else if ("MEMORY_USAGE".equalsIgnoreCase(metricType)) {
            if (value > 90) {
                severity = Severity.CRITICAL;
                description = "Memory usage critically high: " + value;
            } else if (value > 75) {
                severity = Severity.HIGH;
                description = "Memory usage high: " + value;
            }
        }

        if (severity != null) {
            return Optional.of(AnomalyEvent.builder()
                    .deviceId(telemetry.getDeviceId())
                    .severity(severity)
                    .detectedAt(Instant.ofEpochMilli(telemetry.getTimestamp()))
                    .ruleTriggered(this.getClass().getSimpleName())
                    .description(description)
                    .telemetryValue(value)
                    .metricType(metricType)
                    .build());
        }

        return Optional.empty();
    }
}
