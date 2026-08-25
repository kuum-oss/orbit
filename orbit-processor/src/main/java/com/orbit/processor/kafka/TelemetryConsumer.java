package com.orbit.processor.kafka;

import com.orbit.processor.domain.DeviceTelemetry;
import com.orbit.processor.service.AnomalyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);
    private final AnomalyService anomalyService;

    public TelemetryConsumer(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    @KafkaListener(topics = "device-telemetry", groupId = "orbit-processor")
    public void consume(DeviceTelemetry telemetry) {
        log.debug("Received telemetry for device {}: {}={}", telemetry.getDeviceId(), telemetry.getMetricType(), telemetry.getValue());
        anomalyService.processTelemetry(telemetry);
    }
}
