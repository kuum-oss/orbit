package com.orbit.processor.controller;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.service.AnomalyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/anomalies")
public class AnomalyController {

    private final AnomalyService anomalyService;

    public AnomalyController(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    @GetMapping
    public List<AnomalyEvent> getRecentAnomalies(@RequestParam(defaultValue = "PT1H") String duration) {
        return anomalyService.getRecentAnomalies(Duration.parse(duration));
    }

    @GetMapping("/device/{deviceId}")
    public List<AnomalyEvent> getAnomaliesByDevice(@PathVariable String deviceId) {
        return anomalyService.getAnomaliesByDevice(deviceId);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<AnomalyEvent> recent = anomalyService.getRecentAnomalies(Duration.ofHours(24));
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLast24h", recent.size());

        long criticalCount = recent.stream()
                .filter(a -> a.getSeverity().name().equals("CRITICAL"))
                .count();
        stats.put("criticalLast24h", criticalCount);

        return stats;
    }
}
