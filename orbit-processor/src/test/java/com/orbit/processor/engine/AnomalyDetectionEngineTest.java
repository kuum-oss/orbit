package com.orbit.processor.engine;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.DeviceTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AnomalyDetectionEngineTest {

    @Mock
    private AnomalyRule rule1;

    @Mock
    private AnomalyRule rule2;

    private AnomalyDetectionEngine engine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        engine = new AnomalyDetectionEngine(Arrays.asList(rule1, rule2));
    }

    @Test
    void shouldAggregateAnomaliesFromMultipleRules() {
        DeviceTelemetry telemetry = new DeviceTelemetry();
        AnomalyEvent event1 = new AnomalyEvent();
        AnomalyEvent event2 = new AnomalyEvent();

        when(rule1.evaluate(any())).thenReturn(Optional.of(event1));
        when(rule2.evaluate(any())).thenReturn(Optional.of(event2));

        List<AnomalyEvent> results = engine.detect(telemetry);

        assertEquals(2, results.size());
    }
}
