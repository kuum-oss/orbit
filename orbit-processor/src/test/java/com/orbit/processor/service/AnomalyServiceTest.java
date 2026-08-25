package com.orbit.processor.service;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.DeviceTelemetry;
import com.orbit.processor.engine.AnomalyDetectionEngine;
import com.orbit.processor.kafka.AnomalyEventProducer;
import com.orbit.processor.repository.AnomalyEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnomalyServiceTest {

    @Mock
    private AnomalyDetectionEngine engine;

    @Mock
    private AnomalyEventRepository repository;

    @Mock
    private AnomalyEventProducer producer;

    private AnomalyService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AnomalyService(engine, repository, producer);
    }

    @Test
    void shouldProcessTelemetryAndPublishAnomalies() {
        DeviceTelemetry telemetry = new DeviceTelemetry();
        AnomalyEvent event = new AnomalyEvent();
        
        when(engine.detect(any())).thenReturn(Collections.singletonList(event));
        when(repository.save(any())).thenReturn(event);

        List<AnomalyEvent> results = service.processTelemetry(telemetry);

        assertEquals(1, results.size());
        verify(repository).save(event);
        verify(producer).publish(event);
    }
}
