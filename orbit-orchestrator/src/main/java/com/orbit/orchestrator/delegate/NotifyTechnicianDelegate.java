package com.orbit.orchestrator.delegate;

import com.orbit.orchestrator.domain.TicketStatus;
import com.orbit.orchestrator.service.MaintenanceTicketService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("notifyTechnicianDelegate")
public class NotifyTechnicianDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(NotifyTechnicianDelegate.class);
    private final MaintenanceTicketService ticketService;

    public NotifyTechnicianDelegate(MaintenanceTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String deviceId = (String) execution.getVariable("deviceId");
        String ticketIdStr = (String) execution.getVariable("ticketId");
        String assignedTechnician = (String) execution.getVariable("assignedTechnician");
        String severity = (String) execution.getVariable("severity");

        log.info("DISPATCH NOTIFICATION: Alert sent to technician '{}' for ticket '{}' on device '{}' [Severity: {}]",
                assignedTechnician, ticketIdStr, deviceId, severity);

        if (ticketIdStr != null && !ticketIdStr.isBlank()) {
            UUID ticketId = UUID.fromString(ticketIdStr);
            ticketService.updateStatus(ticketId, TicketStatus.WAITING_CONFIRMATION);
        }

        execution.setVariable("notificationDispatched", true);
    }
}
