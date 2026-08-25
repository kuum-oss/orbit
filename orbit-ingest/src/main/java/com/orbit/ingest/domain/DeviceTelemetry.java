package com.orbit.ingest.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("device_telemetry")
public class DeviceTelemetry {
    @Id
    private UUID id;
    private String deviceId;
    private long timestamp;
    private String metricType;
    private double value;
    private double latitude;
    private double longitude;
    private Instant receivedAt;

    public DeviceTelemetry() {
    }

    private DeviceTelemetry(Builder builder) {
        this.id = builder.id;
        this.deviceId = builder.deviceId;
        this.timestamp = builder.timestamp;
        this.metricType = builder.metricType;
        this.value = builder.value;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.receivedAt = builder.receivedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String deviceId;
        private long timestamp;
        private String metricType;
        private double value;
        private double latitude;
        private double longitude;
        private Instant receivedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        public Builder metricType(String metricType) { this.metricType = metricType; return this; }
        public Builder value(double value) { this.value = value; return this; }
        public Builder latitude(double latitude) { this.latitude = latitude; return this; }
        public Builder longitude(double longitude) { this.longitude = longitude; return this; }
        public Builder receivedAt(Instant receivedAt) { this.receivedAt = receivedAt; return this; }

        public DeviceTelemetry build() {
            return new DeviceTelemetry(this);
        }
    }
}

