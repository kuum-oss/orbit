package com.orbit.orchestrator.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("evaluateSeverityDelegate")
public class EvaluateSeverityDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(EvaluateSeverityDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String deviceId = (String) execution.getVariable("deviceId");
        String severityStr = (String) execution.getVariable("severity");

        if (severityStr == null) {
            severityStr = "LOW";
            execution.setVariable("severity", severityStr);
        }

        boolean isCritical = "HIGH".equalsIgnoreCase(severityStr) || "CRITICAL".equalsIgnoreCase(severityStr);
        execution.setVariable("isCritical", isCritical);

        log.info("Evaluated severity for device '{}': severity='{}', isCritical={}",
                deviceId, severityStr, isCritical);
    }
}
