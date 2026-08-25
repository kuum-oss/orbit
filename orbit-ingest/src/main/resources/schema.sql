CREATE TABLE IF NOT EXISTS device_telemetry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id VARCHAR(255) NOT NULL,
    timestamp BIGINT NOT NULL,
    metric_type VARCHAR(255) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);
