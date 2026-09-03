package com.orbit.orchestrator.controller;

import com.orbit.orchestrator.domain.MaintenanceTicket;
import com.orbit.orchestrator.domain.Severity;
import com.orbit.orchestrator.domain.TicketStatus;
import com.orbit.orchestrator.service.MaintenanceTicketService;
import com.orbit.orchestrator.service.OrchestrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceTicketController.class)
class MaintenanceTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaintenanceTicketService ticketService;

    @MockBean
    private OrchestrationService orchestrationService;

    @Test
    @DisplayName("GET /api/v1/tickets should return list of tickets")
    void listTickets() throws Exception {
        UUID id = UUID.randomUUID();
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .ticketId(id)
                .deviceId("device-01")
                .status(TicketStatus.OPEN)
                .severity(Severity.HIGH)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(ticketService.getAllTickets("device-01", TicketStatus.OPEN))
                .thenReturn(List.of(ticket));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("deviceId", "device-01")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketId").value(id.toString()))
                .andExpect(jsonPath("$[0].deviceId").value("device-01"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/v1/tickets/{ticketId} returns ticket if found")
    void getTicketFound() throws Exception {
        UUID id = UUID.randomUUID();
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .ticketId(id)
                .deviceId("device-02")
                .status(TicketStatus.ASSIGNED)
                .assignedTechnician("tech-01")
                .build();

        when(ticketService.getTicket(id)).thenReturn(Optional.of(ticket));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(id.toString()))
                .andExpect(jsonPath("$.assignedTechnician").value("tech-01"));
    }

    @Test
    @DisplayName("GET /api/v1/tickets/{ticketId} returns 404 when not found")
    void getTicketNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.getTicket(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/tickets/stats returns tickets statistics")
    void getStats() throws Exception {
        when(ticketService.getTicketStats()).thenReturn(Map.of(
                "totalTickets", 10L,
                "openTickets", 4L,
                "closedTickets", 6L
        ));

        mockMvc.perform(get("/api/v1/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(10))
                .andExpect(jsonPath("$.openTickets").value(4))
                .andExpect(jsonPath("$.closedTickets").value(6));
    }

    @Test
    @DisplayName("POST /api/v1/tickets/{ticketId}/assign assigns technician")
    void assignTechnicianSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        MaintenanceTicket updated = MaintenanceTicket.builder()
                .ticketId(id)
                .assignedTechnician("tech-thermo-alpha")
                .status(TicketStatus.ASSIGNED)
                .build();

        when(ticketService.assignTechnician(id, "tech-thermo-alpha")).thenReturn(updated);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/assign", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\":\"tech-thermo-alpha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTechnician").value("tech-thermo-alpha"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(ticketService).assignTechnician(id, "tech-thermo-alpha");
    }

    @Test
    @DisplayName("POST /api/v1/tickets/{ticketId}/assign returns 400 when technicianId missing")
    void assignTechnicianBadRequest() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/assign", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/tickets/{ticketId}/confirm returns 409 when no waiting task found")
    void confirmMaintenanceConflict() throws Exception {
        UUID id = UUID.randomUUID();
        when(orchestrationService.confirmMaintenance(eq(id), any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/confirm", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\":\"tech-01\",\"notes\":\"done\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("No active waiting confirmation task found for this ticket"));
    }

    @Test
    @DisplayName("POST /api/v1/tickets/{ticketId}/close closes ticket manually")
    void closeTicket() throws Exception {
        UUID id = UUID.randomUUID();
        MaintenanceTicket closed = MaintenanceTicket.builder()
                .ticketId(id)
                .status(TicketStatus.CLOSED)
                .resolutionNotes("Admin closed")
                .build();

        when(ticketService.closeTicket(id, "Admin closed")).thenReturn(closed);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/close", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Admin closed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.resolutionNotes").value("Admin closed"));
    }
}
