package com.orbit.processor.repository;

import com.orbit.processor.domain.AnomalyEvent;
import com.orbit.processor.domain.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnomalyEventRepository extends JpaRepository<AnomalyEvent, UUID> {
    List<AnomalyEvent> findByDeviceId(String deviceId);
    List<AnomalyEvent> findBySeverity(Severity severity);
    List<AnomalyEvent> findByDetectedAtBetween(Instant from, Instant to);
}
