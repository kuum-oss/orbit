package com.orbit.processor.controller;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.Severity;
import com.orbit.processor.service.AnomalyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnomalyController.class)
class AnomalyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnomalyService anomalyService;

    @Test
    void testGetRecentAnomalies() throws Exception {
        AnomalyEvent event = AnomalyEvent.builder()
                .eventId(UUID.randomUUID())
                .deviceId("atm-001")
                .severity(Severity.CRITICAL)
                .detectedAt(Instant.now())
                .ruleTriggered("ThresholdRule")
                .description("Temperature critically high")
                .telemetryValue(95.0)
                .metricType("TEMPERATURE")
                .build();

        when(anomalyService.getRecentAnomalies(any(Duration.class)))
                .thenReturn(Collections.singletonList(event));

        mockMvc.perform(get("/api/v1/anomalies")
                        .param("duration", "PT1H")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value("atm-001"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));
    }

    @Test
    void testGetAnomaliesByDevice() throws Exception {
        AnomalyEvent event = AnomalyEvent.builder()
                .eventId(UUID.randomUUID())
                .deviceId("pos-123")
                .severity(Severity.HIGH)
                .detectedAt(Instant.now())
                .ruleTriggered("ThresholdRule")
                .description("Battery low")
                .telemetryValue(15.0)
                .metricType("BATTERY_LEVEL")
                .build();

        when(anomalyService.getAnomaliesByDevice(eq("pos-123")))
                .thenReturn(Collections.singletonList(event));

        mockMvc.perform(get("/api/v1/anomalies/device/pos-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value("pos-123"))
                .andExpect(jsonPath("$[0].metricType").value("BATTERY_LEVEL"));
    }

    @Test
    void testGetStats() throws Exception {
        AnomalyEvent critical = AnomalyEvent.builder()
                .severity(Severity.CRITICAL)
                .build();
        AnomalyEvent high = AnomalyEvent.builder()
                .severity(Severity.HIGH)
                .build();

        when(anomalyService.getRecentAnomalies(any(Duration.class)))
                .thenReturn(java.util.Arrays.asList(critical, high));

        mockMvc.perform(get("/api/v1/anomalies/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLast24h").value(2))
                .andExpect(jsonPath("$.criticalLast24h").value(1));
    }
}
