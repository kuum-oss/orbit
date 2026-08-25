package com.orbit.processor.engine;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.DeviceTelemetry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AnomalyDetectionEngine {

    private final List<AnomalyRule> rules;

    public AnomalyDetectionEngine(List<AnomalyRule> rules) {
        this.rules = rules;
    }

    public List<AnomalyEvent> detect(DeviceTelemetry telemetry) {
        return rules.stream()
                .map(rule -> rule.evaluate(telemetry))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
