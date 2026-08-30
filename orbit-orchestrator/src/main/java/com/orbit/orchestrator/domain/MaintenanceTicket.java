package com.orbit.orchestrator.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_tickets")
public class MaintenanceTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID ticketId;

    private String deviceId;

    private UUID createdFromEvent;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    private String assignedTechnician;

    private String processInstanceId;

    private String description;

    private String ruleTriggered;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant resolvedAt;

    private String resolutionNotes;

    public MaintenanceTicket() {
    }

    public MaintenanceTicket(UUID ticketId, String deviceId, UUID createdFromEvent, Severity severity,
                             TicketStatus status, String assignedTechnician, String processInstanceId,
                             String description, String ruleTriggered, Instant createdAt, Instant updatedAt,
                             Instant resolvedAt, String resolutionNotes) {
        this.ticketId = ticketId;
        this.deviceId = deviceId;
        this.createdFromEvent = createdFromEvent;
        this.severity = severity;
        this.status = status;
        this.assignedTechnician = assignedTechnician;
        this.processInstanceId = processInstanceId;
        this.description = description;
        this.ruleTriggered = ruleTriggered;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
        this.resolutionNotes = resolutionNotes;
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = TicketStatus.OPEN;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public UUID getCreatedFromEvent() {
        return createdFromEvent;
    }

    public void setCreatedFromEvent(UUID createdFromEvent) {
        this.createdFromEvent = createdFromEvent;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(String assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRuleTriggered() {
        return ruleTriggered;
    }

    public void setRuleTriggered(String ruleTriggered) {
        this.ruleTriggered = ruleTriggered;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public static MaintenanceTicketBuilder builder() {
        return new MaintenanceTicketBuilder();
    }

    public static class MaintenanceTicketBuilder {
        private UUID ticketId;
        private String deviceId;
        private UUID createdFromEvent;
        private Severity severity;
        private TicketStatus status;
        private String assignedTechnician;
        private String processInstanceId;
        private String description;
        private String ruleTriggered;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant resolvedAt;
        private String resolutionNotes;

        public MaintenanceTicketBuilder ticketId(UUID ticketId) {
            this.ticketId = ticketId;
            return this;
        }

        public MaintenanceTicketBuilder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public MaintenanceTicketBuilder createdFromEvent(UUID createdFromEvent) {
            this.createdFromEvent = createdFromEvent;
            return this;
        }

        public MaintenanceTicketBuilder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public MaintenanceTicketBuilder status(TicketStatus status) {
            this.status = status;
            return this;
        }

        public MaintenanceTicketBuilder assignedTechnician(String assignedTechnician) {
            this.assignedTechnician = assignedTechnician;
            return this;
        }

        public MaintenanceTicketBuilder processInstanceId(String processInstanceId) {
            this.processInstanceId = processInstanceId;
            return this;
        }

        public MaintenanceTicketBuilder description(String description) {
            this.description = description;
            return this;
        }

        public MaintenanceTicketBuilder ruleTriggered(String ruleTriggered) {
            this.ruleTriggered = ruleTriggered;
            return this;
        }

        public MaintenanceTicketBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MaintenanceTicketBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public MaintenanceTicketBuilder resolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public MaintenanceTicketBuilder resolutionNotes(String resolutionNotes) {
            this.resolutionNotes = resolutionNotes;
            return this;
        }

        public MaintenanceTicket build() {
            return new MaintenanceTicket(ticketId, deviceId, createdFromEvent, severity, status,
                    assignedTechnician, processInstanceId, description, ruleTriggered, createdAt,
                    updatedAt, resolvedAt, resolutionNotes);
        }
    }
}
