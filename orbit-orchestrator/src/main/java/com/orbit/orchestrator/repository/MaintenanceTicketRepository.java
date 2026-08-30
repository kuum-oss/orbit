package com.orbit.orchestrator.repository;

import com.orbit.orchestrator.domain.MaintenanceTicket;
import com.orbit.orchestrator.domain.Severity;
import com.orbit.orchestrator.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID> {

    List<MaintenanceTicket> findByDeviceId(String deviceId);

    List<MaintenanceTicket> findByStatus(TicketStatus status);

    List<MaintenanceTicket> findBySeverity(Severity severity);

    List<MaintenanceTicket> findByDeviceIdAndStatus(String deviceId, TicketStatus status);

    Optional<MaintenanceTicket> findByProcessInstanceId(String processInstanceId);

    Optional<MaintenanceTicket> findByCreatedFromEvent(UUID eventId);

    long countByStatus(TicketStatus status);

    long countBySeverity(Severity severity);

    List<MaintenanceTicket> findTop20ByOrderByCreatedAtDesc();
}
