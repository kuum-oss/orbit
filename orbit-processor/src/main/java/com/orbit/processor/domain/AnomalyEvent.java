package com.orbit.processor.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anomaly_events")
public class AnomalyEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventId;

    private String deviceId;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private Instant detectedAt;

    private String ruleTriggered;

    private String description;

    private double telemetryValue;

    private String metricType;

    public AnomalyEvent() {
    }

    public AnomalyEvent(UUID eventId, String deviceId, Severity severity, Instant detectedAt,
                         String ruleTriggered, String description, double telemetryValue, String metricType) {
        this.eventId = eventId;
        this.deviceId = deviceId;
        this.severity = severity;
        this.detectedAt = detectedAt;
        this.ruleTriggered = ruleTriggered;
        this.description = description;
        this.telemetryValue = telemetryValue;
        this.metricType = metricType;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public String getRuleTriggered() { return ruleTriggered; }
    public void setRuleTriggered(String ruleTriggered) { this.ruleTriggered = ruleTriggered; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getTelemetryValue() { return telemetryValue; }
    public void setTelemetryValue(double telemetryValue) { this.telemetryValue = telemetryValue; }

    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }

    public static AnomalyEventBuilder builder() { return new AnomalyEventBuilder(); }

    public static class AnomalyEventBuilder {
        private UUID eventId;
        private String deviceId;
        private Severity severity;
        private Instant detectedAt;
        private String ruleTriggered;
        private String description;
        private double telemetryValue;
        private String metricType;

        public AnomalyEventBuilder eventId(UUID eventId) { this.eventId = eventId; return this; }
        public AnomalyEventBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public AnomalyEventBuilder severity(Severity severity) { this.severity = severity; return this; }
        public AnomalyEventBuilder detectedAt(Instant detectedAt) { this.detectedAt = detectedAt; return this; }
        public AnomalyEventBuilder ruleTriggered(String ruleTriggered) { this.ruleTriggered = ruleTriggered; return this; }
        public AnomalyEventBuilder description(String description) { this.description = description; return this; }
        public AnomalyEventBuilder telemetryValue(double telemetryValue) { this.telemetryValue = telemetryValue; return this; }
        public AnomalyEventBuilder metricType(String metricType) { this.metricType = metricType; return this; }

        public AnomalyEvent build() {
            return new AnomalyEvent(eventId, deviceId, severity, detectedAt, ruleTriggered, description, telemetryValue, metricType);
        }
    }
}
