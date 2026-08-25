package com.orbit.ingest.repository;

import com.orbit.ingest.domain.DeviceTelemetry;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TelemetryRepository extends ReactiveCrudRepository<DeviceTelemetry, UUID> {
}
