package com.orbit.orchestrator.delegate;

import com.orbit.orchestrator.domain.Severity;
import com.orbit.orchestrator.service.MaintenanceTicketService;
import com.orbit.orchestrator.service.TechnicianAssignmentService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("assignTechnicianDelegate")
public class AssignTechnicianDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(AssignTechnicianDelegate.class);

    private final TechnicianAssignmentService assignmentService;
    private final MaintenanceTicketService ticketService;

    public AssignTechnicianDelegate(TechnicianAssignmentService assignmentService,
                                    MaintenanceTicketService ticketService) {
        this.assignmentService = assignmentService;
        this.ticketService = ticketService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String deviceId = (String) execution.getVariable("deviceId");
        String metricType = (String) execution.getVariable("metricType");
        String severityStr = (String) execution.getVariable("severity");
        String ticketIdStr = (String) execution.getVariable("ticketId");

        Severity severity = Severity.HIGH;
        if (severityStr != null) {
            try {
                severity = Severity.valueOf(severityStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String technicianId = assignmentService.assignTechnician(deviceId, severity, metricType);
        execution.setVariable("assignedTechnician", technicianId);

        if (ticketIdStr != null && !ticketIdStr.isBlank()) {
            UUID ticketId = UUID.fromString(ticketIdStr);
            ticketService.assignTechnician(ticketId, technicianId);
        }

        log.info("Assigned technician '{}' for ticket '{}' (device '{}')",
                technicianId, ticketIdStr, deviceId);
    }
}
