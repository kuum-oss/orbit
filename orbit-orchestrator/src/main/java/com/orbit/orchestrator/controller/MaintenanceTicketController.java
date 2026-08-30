package com.orbit.orchestrator.controller;

import com.orbit.orchestrator.domain.MaintenanceTicket;
import com.orbit.orchestrator.domain.TicketStatus;
import com.orbit.orchestrator.service.MaintenanceTicketService;
import com.orbit.orchestrator.service.OrchestrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class MaintenanceTicketController {

    private final MaintenanceTicketService ticketService;
    private final OrchestrationService orchestrationService;

    public MaintenanceTicketController(MaintenanceTicketService ticketService,
                                       OrchestrationService orchestrationService) {
        this.ticketService = ticketService;
        this.orchestrationService = orchestrationService;
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceTicket>> listTickets(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) TicketStatus status) {
        List<MaintenanceTicket> tickets = ticketService.getAllTickets(deviceId, status);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<MaintenanceTicket> getTicket(@PathVariable UUID ticketId) {
        return ticketService.getTicket(ticketId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(ticketService.getTicketStats());
    }

    @PostMapping("/{ticketId}/assign")
    public ResponseEntity<MaintenanceTicket> assignTechnician(
            @PathVariable UUID ticketId,
            @RequestBody Map<String, String> request) {
        String technicianId = request.get("technicianId");
        if (technicianId == null || technicianId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        MaintenanceTicket updated = ticketService.assignTechnician(ticketId, technicianId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{ticketId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmMaintenance(
            @PathVariable UUID ticketId,
            @RequestBody(required = false) Map<String, String> request) {
        String technicianId = (request != null && request.containsKey("technicianId"))
                ? request.get("technicianId") : "tech-assigned";
        String notes = (request != null && request.containsKey("notes"))
                ? request.get("notes") : "Maintenance completed and confirmed by technician";

        boolean completed = orchestrationService.confirmMaintenance(ticketId, technicianId, notes);
        if (completed) {
            return ResponseEntity.ok(Map.of(
                    "ticketId", ticketId,
                    "status", "CONFIRMED_AND_COMPLETED",
                    "confirmedBy", technicianId
            ));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "ticketId", ticketId,
                    "error", "No active waiting confirmation task found for this ticket"
            ));
        }
    }

    @PostMapping("/{ticketId}/close")
    public ResponseEntity<MaintenanceTicket> closeTicket(
            @PathVariable UUID ticketId,
            @RequestBody(required = false) Map<String, String> request) {
        String notes = (request != null && request.containsKey("notes"))
                ? request.get("notes") : "Manually closed by administrator";
        MaintenanceTicket closed = ticketService.closeTicket(ticketId, notes);
        return ResponseEntity.ok(closed);
    }
}
