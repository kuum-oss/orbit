package com.orbit.ingest.service;

import com.orbit.ingest.domain.DeviceTelemetry;
import com.orbit.ingest.kafka.TelemetryKafkaProducer;
import com.orbit.ingest.repository.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private TelemetryKafkaProducer kafkaProducer;

    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        telemetryService = new TelemetryService(telemetryRepository, kafkaProducer);
    }

    @Test
    void processTelemetry_shouldSaveAndPublish() {
        DeviceTelemetry telemetry = new DeviceTelemetry();
        telemetry.setDeviceId("device-1");

        DeviceTelemetry savedTelemetry = new DeviceTelemetry();
        savedTelemetry.setId(UUID.randomUUID());
        savedTelemetry.setDeviceId("device-1");
        savedTelemetry.setReceivedAt(Instant.now());

        when(telemetryRepository.save(any(DeviceTelemetry.class))).thenReturn(Mono.just(savedTelemetry));
        when(kafkaProducer.publish(any(DeviceTelemetry.class))).thenReturn(Mono.empty());

        Mono<DeviceTelemetry> result = telemetryService.processTelemetry(telemetry);

        StepVerifier.create(result)
                .expectNext(savedTelemetry)
                .verifyComplete();

        verify(telemetryRepository).save(any(DeviceTelemetry.class));
        verify(kafkaProducer).publish(savedTelemetry);
    }

    @Test
    void processBatch_shouldProcessMultiple() {
        DeviceTelemetry telemetry1 = new DeviceTelemetry();
        telemetry1.setDeviceId("device-1");

        DeviceTelemetry telemetry2 = new DeviceTelemetry();
        telemetry2.setDeviceId("device-2");

        when(telemetryRepository.save(any(DeviceTelemetry.class))).thenAnswer(invocation -> {
            DeviceTelemetry t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return Mono.just(t);
        });
        when(kafkaProducer.publish(any(DeviceTelemetry.class))).thenReturn(Mono.empty());

        Flux<DeviceTelemetry> input = Flux.just(telemetry1, telemetry2);

        Flux<DeviceTelemetry> result = telemetryService.processBatch(input);

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }
}
