package com.orbit.ingest.kafka;

import com.orbit.ingest.domain.DeviceTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TelemetryKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryKafkaProducer.class);
    private final KafkaTemplate<String, DeviceTelemetry> kafkaTemplate;
    private static final String TOPIC = "device-telemetry";

    public TelemetryKafkaProducer(KafkaTemplate<String, DeviceTelemetry> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<Void> publish(DeviceTelemetry telemetry) {
        return Mono.fromFuture(kafkaTemplate.send(TOPIC, telemetry.getDeviceId(), telemetry))
                .doOnSuccess(result -> log.debug("Sent telemetry for device {}", telemetry.getDeviceId()))
                .doOnError(e -> log.error("Failed to send telemetry for device {}", telemetry.getDeviceId(), e))
                .then();
    }
}
