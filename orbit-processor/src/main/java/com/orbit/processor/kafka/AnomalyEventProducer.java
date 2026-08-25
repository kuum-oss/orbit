package com.orbit.processor.kafka;

import com.orbit.processor.domain.AnomalyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnomalyEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AnomalyEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "anomaly-events";

    public AnomalyEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(AnomalyEvent event) {
        log.info("Publishing anomaly event for device {}: {}", event.getDeviceId(), event.getDescription());
        kafkaTemplate.send(TOPIC, event.getDeviceId(), event);
    }
}
