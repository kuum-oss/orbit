package com.orbit.orchestrator.delegate;

import com.orbit.orchestrator.domain.MaintenanceTicket;
import com.orbit.orchestrator.domain.Severity;
import com.orbit.orchestrator.service.MaintenanceTicketService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("createMaintenanceTicketDelegate")
public class CreateMaintenanceTicketDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CreateMaintenanceTicketDelegate.class);
    private final MaintenanceTicketService ticketService;

    public CreateMaintenanceTicketDelegate(MaintenanceTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String deviceId = (String) execution.getVariable("deviceId");
        String eventIdStr = (String) execution.getVariable("eventId");
        String severityStr = (String) execution.getVariable("severity");
        String ruleTriggered = (String) execution.getVariable("ruleTriggered");
        String description = (String) execution.getVariable("description");
        String processInstanceId = execution.getProcessInstanceId();

        UUID eventId = null;
        if (eventIdStr != null && !eventIdStr.isBlank()) {
            try {
                eventId = UUID.fromString(eventIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid eventId format: {}", eventIdStr);
            }
        }

        Severity severity = Severity.HIGH;
        if (severityStr != null) {
            try {
                severity = Severity.valueOf(severityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                severity = Severity.HIGH;
            }
        }

        MaintenanceTicket ticket = ticketService.createTicket(
                deviceId,
                eventId,
                severity,
                ruleTriggered != null ? ruleTriggered : "AUTOMATIC_DETECTION",
                description != null ? description : "Maintenance ticket triggered by anomaly event",
                processInstanceId
        );

        execution.setVariable("ticketId", ticket.getTicketId().toString());
        log.info("Ticket {} successfully created for process instance {}", ticket.getTicketId(), processInstanceId);
    }
}
