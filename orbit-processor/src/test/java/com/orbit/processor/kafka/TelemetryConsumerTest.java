package com.orbit.processor.kafka;

import com.orbit.processor.domain.DeviceTelemetry;
import com.orbit.processor.service.AnomalyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

class TelemetryConsumerTest {

    @Mock
    private AnomalyService anomalyService;

    private TelemetryConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new TelemetryConsumer(anomalyService);
    }

    @Test
    void shouldPassTelemetryToService() {
        DeviceTelemetry telemetry = new DeviceTelemetry();
        consumer.consume(telemetry);
        verify(anomalyService).processTelemetry(telemetry);
    }
}
