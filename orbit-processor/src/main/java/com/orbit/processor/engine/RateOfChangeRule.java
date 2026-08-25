package com.orbit.processor.engine;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.DeviceTelemetry;
import com.orbit.processor.domain.Severity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateOfChangeRule implements AnomalyRule {

    private final Map<String, Queue<DeviceTelemetry>> deviceHistory = new ConcurrentHashMap<>();
    
    @Value("${orbit.processor.rules.rate-of-change.window-size:5}")
    private int windowSize;

    @Value("${orbit.processor.rules.rate-of-change.threshold.TEMPERATURE:15.0}")
    private double temperatureChangeThreshold;

    @Override
    public Optional<AnomalyEvent> evaluate(DeviceTelemetry telemetry) {
        String key = telemetry.getDeviceId() + "_" + telemetry.getMetricType();
        Queue<DeviceTelemetry> history = deviceHistory.computeIfAbsent(key, k -> new LinkedList<>());
        
        synchronized (history) {
            if (!history.isEmpty()) {
                DeviceTelemetry previous = history.peek(); // Compare with the oldest in the window
                double changeRate = Math.abs(telemetry.getValue() - previous.getValue());
                long timeDiff = telemetry.getTimestamp() - previous.getTimestamp();

                // Simple check: absolute change greater than threshold
                if ("TEMPERATURE".equalsIgnoreCase(telemetry.getMetricType()) && changeRate > temperatureChangeThreshold) {
                    history.add(telemetry);
                    if (history.size() > windowSize) {
                        history.poll();
                    }
                    return Optional.of(AnomalyEvent.builder()
                            .deviceId(telemetry.getDeviceId())
                            .severity(Severity.HIGH)
                            .detectedAt(Instant.ofEpochMilli(telemetry.getTimestamp()))
                            .ruleTriggered(this.getClass().getSimpleName())
                            .description(String.format("Sudden change in %s: %.2f in %d ms", telemetry.getMetricType(), changeRate, timeDiff))
                            .telemetryValue(telemetry.getValue())
                            .metricType(telemetry.getMetricType())
                            .build());
                }
            }

            history.add(telemetry);
            if (history.size() > windowSize) {
                history.poll();
            }
        }
        
        return Optional.empty();
    }
}
