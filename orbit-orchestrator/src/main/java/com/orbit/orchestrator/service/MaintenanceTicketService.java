package com.orbit.orchestrator.service;

import com.orbit.orchestrator.domain.MaintenanceTicket;
import com.orbit.orchestrator.domain.Severity;
import com.orbit.orchestrator.domain.TicketStatus;
import com.orbit.orchestrator.repository.MaintenanceTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MaintenanceTicketService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceTicketService.class);
    private final MaintenanceTicketRepository ticketRepository;

    public MaintenanceTicketService(MaintenanceTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public MaintenanceTicket createTicket(String deviceId, UUID eventId, Severity severity,
                                          String ruleTriggered, String description, String processInstanceId) {
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .deviceId(deviceId)
                .createdFromEvent(eventId)
                .severity(severity)
                .ruleTriggered(ruleTriggered)
                .description(description)
                .processInstanceId(processInstanceId)
                .status(TicketStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        MaintenanceTicket saved = ticketRepository.save(ticket);
        log.info("Created maintenance ticket {} for device {} with severity {}", saved.getTicketId(), deviceId, severity);
        return saved;
    }

    public MaintenanceTicket assignTechnician(UUID ticketId, String technicianId) {
        MaintenanceTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        ticket.setAssignedTechnician(technicianId);
        ticket.setStatus(TicketStatus.ASSIGNED);
        ticket.setUpdatedAt(Instant.now());
        log.info("Assigned ticket {} to technician {}", ticketId, technicianId);
        return ticketRepository.save(ticket);
    }

    public MaintenanceTicket updateStatus(UUID ticketId, TicketStatus status) {
        MaintenanceTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        ticket.setStatus(status);
        ticket.setUpdatedAt(Instant.now());
        log.info("Updated ticket {} status to {}", ticketId, status);
        return ticketRepository.save(ticket);
    }

    public MaintenanceTicket closeTicket(UUID ticketId, String resolutionNotes) {
        MaintenanceTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setResolvedAt(Instant.now());
        ticket.setResolutionNotes(resolutionNotes);
        ticket.setUpdatedAt(Instant.now());
        log.info("Closed ticket {} with resolution notes: {}", ticketId, resolutionNotes);
        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public Optional<MaintenanceTicket> getTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId);
    }

    @Transactional(readOnly = true)
    public Optional<MaintenanceTicket> getTicketByProcessInstanceId(String processInstanceId) {
        return ticketRepository.findByProcessInstanceId(processInstanceId);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getAllTickets(String deviceId, TicketStatus status) {
        if (deviceId != null && status != null) {
            return ticketRepository.findByDeviceIdAndStatus(deviceId, status);
        } else if (deviceId != null) {
            return ticketRepository.findByDeviceId(deviceId);
        } else if (status != null) {
            return ticketRepository.findByStatus(status);
        } else {
            return ticketRepository.findAll();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTicketStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTickets", ticketRepository.count());
        stats.put("openTickets", ticketRepository.countByStatus(TicketStatus.OPEN));
        stats.put("assignedTickets", ticketRepository.countByStatus(TicketStatus.ASSIGNED));
        stats.put("inProgressTickets", ticketRepository.countByStatus(TicketStatus.IN_PROGRESS));
        stats.put("waitingConfirmationTickets", ticketRepository.countByStatus(TicketStatus.WAITING_CONFIRMATION));
        stats.put("closedTickets", ticketRepository.countByStatus(TicketStatus.CLOSED));
        stats.put("criticalTickets", ticketRepository.countBySeverity(Severity.CRITICAL));
        stats.put("highTickets", ticketRepository.countBySeverity(Severity.HIGH));
        stats.put("mediumTickets", ticketRepository.countBySeverity(Severity.MEDIUM));
        stats.put("lowTickets", ticketRepository.countBySeverity(Severity.LOW));
        return stats;
    }
}
