package com.orbit.processor.service;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.DeviceTelemetry;
import com.orbit.processor.engine.AnomalyDetectionEngine;
import com.orbit.processor.kafka.AnomalyEventProducer;
import com.orbit.processor.repository.AnomalyEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AnomalyService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyService.class);
    private final AnomalyDetectionEngine engine;
    private final AnomalyEventRepository repository;
    private final AnomalyEventProducer producer;

    public AnomalyService(AnomalyDetectionEngine engine, AnomalyEventRepository repository, AnomalyEventProducer producer) {
        this.engine = engine;
        this.repository = repository;
        this.producer = producer;
    }

    public List<AnomalyEvent> processTelemetry(DeviceTelemetry telemetry) {
        List<AnomalyEvent> anomalies = engine.detect(telemetry);

        for (AnomalyEvent event : anomalies) {
            AnomalyEvent savedEvent = repository.save(event);
            producer.publish(savedEvent);
            log.warn("Anomaly detected for device {}: {}", savedEvent.getDeviceId(), savedEvent.getDescription());
        }

        return anomalies;
    }

    public List<AnomalyEvent> getAnomaliesByDevice(String deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    public List<AnomalyEvent> getRecentAnomalies(Duration duration) {
        Instant to = Instant.now();
        Instant from = to.minus(duration);
        return repository.findByDetectedAtBetween(from, to);
    }
}
