package com.orbit.orchestrator.service;

import com.orbit.orchestrator.domain.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TechnicianAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TechnicianAssignmentService.class);

    private final Map<String, List<String>> technicianRoster = Map.of(
            "TEMPERATURE", List.of("tech-thermo-alpha", "tech-thermo-beta"),
            "BATTERY_LEVEL", List.of("tech-power-alpha", "tech-power-beta"),
            "CPU_USAGE", List.of("tech-sys-alpha", "tech-sys-beta"),
            "MEMORY_USAGE", List.of("tech-sys-alpha", "tech-sys-gamma"),
            "DEFAULT", List.of("tech-general-01", "tech-general-02", "tech-general-03")
    );

    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    public String assignTechnician(String deviceId, Severity severity, String metricType) {
        String key = (metricType != null && technicianRoster.containsKey(metricType)) ? metricType : "DEFAULT";
        List<String> pool = technicianRoster.get(key);

        AtomicInteger counter = roundRobinCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int index = Math.abs(counter.getAndIncrement() % pool.size());
        String selectedTechnician = pool.get(index);

        log.info("Assigned technician '{}' to device '{}' (metric={}, severity={})",
                selectedTechnician, deviceId, metricType, severity);
        return selectedTechnician;
    }
}
