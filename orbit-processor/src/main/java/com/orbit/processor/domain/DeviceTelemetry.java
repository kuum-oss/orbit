package com.orbit.processor.domain;

public class DeviceTelemetry {
    private String deviceId;
    private long timestamp;
    private String metricType;
    private double value;
    private double latitude;
    private double longitude;

    public DeviceTelemetry() {
    }

    public DeviceTelemetry(String deviceId, long timestamp, String metricType, double value, double latitude, double longitude) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.metricType = metricType;
        this.value = value;
        this.latitude = latitude;
        this.longitude = longitude;
    }

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

    public static DeviceTelemetryBuilder builder() { return new DeviceTelemetryBuilder(); }

    public static class DeviceTelemetryBuilder {
        private String deviceId;
        private long timestamp;
        private String metricType;
        private double value;
        private double latitude;
        private double longitude;

        public DeviceTelemetryBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public DeviceTelemetryBuilder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        public DeviceTelemetryBuilder metricType(String metricType) { this.metricType = metricType; return this; }
        public DeviceTelemetryBuilder value(double value) { this.value = value; return this; }
        public DeviceTelemetryBuilder latitude(double latitude) { this.latitude = latitude; return this; }
        public DeviceTelemetryBuilder longitude(double longitude) { this.longitude = longitude; return this; }

        public DeviceTelemetry build() {
            return new DeviceTelemetry(deviceId, timestamp, metricType, value, latitude, longitude);
        }
    }
}
