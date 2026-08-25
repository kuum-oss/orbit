package com.orbit.ingest.service;

import com.orbit.ingest.domain.DeviceTelemetry;
import com.orbit.ingest.kafka.TelemetryKafkaProducer;
import com.orbit.ingest.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    private final TelemetryRepository telemetryRepository;
    private final TelemetryKafkaProducer kafkaProducer;

    public TelemetryService(TelemetryRepository telemetryRepository, TelemetryKafkaProducer kafkaProducer) {
        this.telemetryRepository = telemetryRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public Mono<DeviceTelemetry> processTelemetry(DeviceTelemetry telemetry) {
        telemetry.setReceivedAt(Instant.now());
        return telemetryRepository.save(telemetry)
                .flatMap(savedTelemetry -> 
                        kafkaProducer.publish(savedTelemetry)
                                .thenReturn(savedTelemetry)
                )
                .doOnError(e -> log.error("Failed to process telemetry", e));
    }

    public Flux<DeviceTelemetry> processBatch(Flux<DeviceTelemetry> telemetryFlux) {
        return telemetryFlux.flatMap(this::processTelemetry);
    }
}
