package com.orbit.orchestrator.kafka;

import com.orbit.orchestrator.domain.AnomalyEvent;
import com.orbit.orchestrator.service.OrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnomalyEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnomalyEventConsumer.class);
    private final OrchestrationService orchestrationService;

    public AnomalyEventConsumer(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @KafkaListener(
            topics = "anomaly-events",
            groupId = "orbit-orchestrator",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(AnomalyEvent event) {
        log.info("Received anomaly event for device '{}' [severity={}, rule={}]",
                event.getDeviceId(), event.getSeverity(), event.getRuleTriggered());

        try {
            orchestrationService.startProcess(event);
        } catch (Exception e) {
            log.error("Failed to start orchestration process for event from device '{}': {}",
                    event.getDeviceId(), e.getMessage(), e);
            throw e;
        }
    }
}
