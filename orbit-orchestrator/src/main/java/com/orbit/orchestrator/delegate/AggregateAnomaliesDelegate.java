package com.orbit.orchestrator.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("aggregateAnomaliesDelegate")
public class AggregateAnomaliesDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(AggregateAnomaliesDelegate.class);

    private final Map<String, List<Instant>> deviceAnomalyHistory = new ConcurrentHashMap<>();

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String deviceId = (String) execution.getVariable("deviceId");
        Instant now = Instant.now();

        deviceAnomalyHistory.compute(deviceId, (key, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(now);
            return list;
        });

        int currentCount = deviceAnomalyHistory.getOrDefault(deviceId, List.of()).size();
        execution.setVariable("aggregatedCount", currentCount);

        if (!execution.hasVariable("aggregationTimeout")) {
            execution.setVariable("aggregationTimeout", "PT15M");
        }

        log.info("Aggregated anomaly for device '{}'. Total recorded in current window: {}",
                deviceId, currentCount);
    }

    public int getAnomalyCount(String deviceId) {
        return deviceAnomalyHistory.getOrDefault(deviceId, List.of()).size();
    }

    public void clearHistory(String deviceId) {
        deviceAnomalyHistory.remove(deviceId);
    }
}
