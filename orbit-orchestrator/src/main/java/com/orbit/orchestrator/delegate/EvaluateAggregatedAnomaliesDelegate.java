package com.orbit.orchestrator.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("evaluateAggregatedAnomaliesDelegate")
public class EvaluateAggregatedAnomaliesDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(EvaluateAggregatedAnomaliesDelegate.class);
    private static final int ESCALATION_THRESHOLD = 3;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String deviceId = (String) execution.getVariable("deviceId");
        Integer count = (Integer) execution.getVariable("aggregatedCount");
        Boolean forceEscalate = (Boolean) execution.getVariable("forceEscalate");

        boolean shouldEscalate = (forceEscalate != null && forceEscalate) ||
                (count != null && count >= ESCALATION_THRESHOLD);

        execution.setVariable("escalate", shouldEscalate);

        log.info("Evaluated aggregated anomalies for device '{}': count={}, forceEscalate={}, escalate={}",
                deviceId, count, forceEscalate, shouldEscalate);
    }
}
