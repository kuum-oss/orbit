package com.orbit.processor.engine;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.DeviceTelemetry;

import java.util.Optional;

public interface AnomalyRule {
    Optional<AnomalyEvent> evaluate(DeviceTelemetry telemetry);
}
