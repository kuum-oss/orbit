package com.orbit.orchestrator.delegate;

import com.orbit.orchestrator.service.MaintenanceTicketService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("closeMaintenanceTicketDelegate")
public class CloseMaintenanceTicketDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CloseMaintenanceTicketDelegate.class);
    private final MaintenanceTicketService ticketService;

    public CloseMaintenanceTicketDelegate(MaintenanceTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String ticketIdStr = (String) execution.getVariable("ticketId");
        String resolutionNotes = (String) execution.getVariable("resolutionNotes");

        if (resolutionNotes == null || resolutionNotes.isBlank()) {
            resolutionNotes = "Maintenance completed and verified by technician.";
        }

        if (ticketIdStr != null && !ticketIdStr.isBlank()) {
            UUID ticketId = UUID.fromString(ticketIdStr);
            ticketService.closeTicket(ticketId, resolutionNotes);
            log.info("Closed maintenance ticket {} with notes: {}", ticketId, resolutionNotes);
        } else {
            log.warn("No ticketId found in execution context to close.");
        }
    }
}
