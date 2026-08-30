package com.orbit.orchestrator.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("discardAnomalyDelegate")
public class DiscardAnomalyDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(DiscardAnomalyDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String deviceId = (String) execution.getVariable("deviceId");
        String eventId = (String) execution.getVariable("eventId");
        String severity = (String) execution.getVariable("severity");

        log.info("DISCARD: Transient anomaly discarded for device '{}' (eventId={}, severity={})",
                deviceId, eventId, severity);

        execution.setVariable("discarded", true);
    }
}
